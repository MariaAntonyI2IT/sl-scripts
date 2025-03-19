package com.mdtlabs.migration.script;

import com.mdtlabs.migration.connection.SqlConnection;
import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;
import org.hl7.fhir.r4.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class PatientStatusUpdate {
    public static String URL;
    public static String BASE_IDENTIFIER;
    private Connection connection =
            SqlConnection.getSqlConnection().getConnection();

    public void updatePatientStatus() throws SQLException {
        RestUtil restUtil = new RestUtil();
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL, Constants.SERVICER_EQUEST, "status=active,on-hold"));
        int count = 0;
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            ServiceRequest serviceRequest = (ServiceRequest) entry.getResource();
            String currentStatus = null;
            String status = null;
            String category = null;
            String encounterType = null;
            for (Identifier identifier : serviceRequest.getIdentifier()) {
                if (StringUtil.concatString(BASE_IDENTIFIER, Constants.PATIENT_CURRENT_STATUS).equals(identifier.getSystem())) {
                    currentStatus = identifier.getValue();
                }
                if (StringUtil.concatString(BASE_IDENTIFIER, Constants.PATIENT_STATUS).equals(identifier.getSystem())) {
                    status = identifier.getValue();
                }
                if (StringUtil.concatString(BASE_IDENTIFIER, Constants.CATEGORY).equals(identifier.getSystem())) {
                    category = identifier.getValue();
                }
                if (StringUtil.concatString(BASE_IDENTIFIER, Constants.ENCOUNTER_TYPE).equals(identifier.getSystem())) {
                    encounterType = identifier.getValue();
                }
            }
            String relatedPerson = serviceRequest.getPerformer().stream().map(Reference::getReference)
                    .filter(reference -> reference.contains(String.valueOf(ResourceType.RelatedPerson))).findFirst()
                    .orElse(null);

            if (Objects.isNull(currentStatus)) {
                currentStatus = status;
            }
            if (!Objects.isNull(currentStatus)) {
                try (PreparedStatement pstmt = connection.prepareStatement(Constants.INSERT_INTO_PATIENT_STATUS)) {
                    pstmt.setString(1, getIdFromResourceUrl(relatedPerson)); // Assuming this returns an integer
                    pstmt.setString(2, currentStatus);  // If it's a string, it's automatically quoted
                    pstmt.setString(3, encounterType);
                    pstmt.setString(4, category);

                    System.out.println(pstmt + "------------" + count++);

                    pstmt.executeUpdate(); // Safe execution
                } catch (SQLException e) {
                    e.printStackTrace(); // Handle SQL exceptions properly
                }
            }

        }


    }

    public String getIdFromResourceUrl(String reference) {
        String[] referencePaths = reference.split(Constants.FORWARD_SLASH);
        return referencePaths.length > 1 ? referencePaths[Constants.ONE] : reference;
    }
}
