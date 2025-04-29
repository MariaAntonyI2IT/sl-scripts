package com.mdtlabs.migration.script;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ResourceType;

import com.mdtlabs.migration.connection.SqlConnection;
import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.ProvenanceDTO;
import com.mdtlabs.migration.util.CsvReportUtil;
import com.mdtlabs.migration.util.FhirUtils;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;

public class LabTestIdUpdate {

    public static String URL;
    public static String BASE_IDENTIFIER;

    private final Connection connection = SqlConnection.getSqlConnection().getConnection();
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String UNKNOWN_LAB_TEST = "UNKNOWN_LAB_TEST";
    public static final String DIAGNOSTIC_REPORT="DiagnosticReport";
    ProvenanceDTO provenance = FhirUtils.getProvenance();

    public void updateDiagnosisData() throws SQLException, IOException {
        String LAB_TEST_NAME_IDENTIFIER = StringUtil.concatString(BASE_IDENTIFIER, "lab-test-name");
        String LAB_TEST_ID_IDENTIFIER = StringUtil.concatString(BASE_IDENTIFIER, "lab-test-id");
        String DIAGNOSTIC_REPORT_URL = "DiagnosticReport?identifier=" + LAB_TEST_NAME_IDENTIFIER + "|&_count=100000";

        Map<String, Long> labTestData = fetchDiagnosticReport();
        RestUtil restUtil = new RestUtil();
        Bundle updatedBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL, DIAGNOSTIC_REPORT_URL));
        List<String> headers = List.of(SUCCESS, FAILURE, UNKNOWN_LAB_TEST);
        List<String[]> data = new ArrayList<>();
        if (Objects.nonNull(bundle)) {
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            entries.stream()
                    .filter(entry -> Objects.nonNull(entry.getResource()) && entry.getResource() instanceof DiagnosticReport)
                    .forEach(entry -> {
                        DiagnosticReport diagnosticReport = (DiagnosticReport) entry.getResource();
                        String[] row = new String[headers.size()];
                        try {
                            if (Objects.nonNull(diagnosticReport.getIdentifier()) &&
                                    diagnosticReport.getIdentifier().stream().noneMatch(id -> id.getSystem().equalsIgnoreCase(LAB_TEST_ID_IDENTIFIER))) {
                                String labTestName = diagnosticReport.getIdentifier().stream()
                                        .filter(identifier -> identifier.getSystem().equalsIgnoreCase(LAB_TEST_NAME_IDENTIFIER))
                                        .map(Identifier::getValue).findFirst().get();

                                Long labTestId = labTestData.get(labTestName);

                                if (Objects.isNull(labTestId)) {
                                    row[2] = diagnosticReport.getIdPart();
                                    data.add(row);
                                    return;
                                }

                                Identifier identifier = new Identifier();
                                identifier.setSystem(LAB_TEST_ID_IDENTIFIER);
                                identifier.setValue(String.valueOf(labTestId));
                                diagnosticReport.addIdentifier(identifier);

                                FhirUtils.setBundle(StringUtil.concatString(String.valueOf(ResourceType.DiagnosticReport), Constants.FORWARD_SLASH, diagnosticReport.getIdPart()),
                                        StringUtil.concatString(String.valueOf(ResourceType.DiagnosticReport), diagnosticReport.getIdPart()),
                                        Bundle.HTTPVerb.PUT, diagnosticReport, updatedBundle, provenance);

                                row[0] = diagnosticReport.getIdPart();
                            }
                        } catch (Exception e) {
                            System.out.println("EXCEPTION OCCURED FOR ID " + diagnosticReport.getIdPart() + e);
                            row[1] = diagnosticReport.getIdPart();
                        }
                        data.add(row);
                    });
        }

        if (Objects.nonNull(updatedBundle) && !updatedBundle.getEntry().isEmpty()) {
            restUtil.saveBundle(updatedBundle);  // Use the updated bundle for saving
        }

        CsvReportUtil.writeDynamicCsv(DIAGNOSTIC_REPORT,headers, data);

    }

    private Map<String, Long> fetchDiagnosticReport() throws SQLException {
        Map<String, Long> labTestData = new HashMap<>();
        String FETCH_LABTEST_DATA = "select test_name,id from lab_test_customization";
        PreparedStatement statement = connection.prepareStatement(FETCH_LABTEST_DATA);
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next()) {
            String labTestName = resultSet.getString("test_name");
            Long labTestId = resultSet.getLong("id");
            labTestData.put(labTestName, labTestId);
        }
        return labTestData;
    }


}
