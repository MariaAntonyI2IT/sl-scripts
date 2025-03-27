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
            String ticketType = null;
            String patientId = null;
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
            if (serviceRequest.getRequisition().getSystem().equals(StringUtil.concatString(BASE_IDENTIFIER, Constants.TICKET_TYPE))) {
                ticketType = serviceRequest.getRequisition().getValue();
            }
            String reason = serviceRequest.getPatientInstruction();
            String relatedPersonId = serviceRequest.getPerformer().stream().map(Reference::getReference)
                    .filter(reference -> reference.contains(String.valueOf(ResourceType.RelatedPerson))).findFirst()
                    .orElse(null);

            if (Objects.isNull(currentStatus)) {
                currentStatus = status;
            }
            String memberId = getIdFromResourceUrl(relatedPersonId);
            Bundle relatedPersonBundle = restUtil.getDataFromFhir(StringUtil.concatString(URL, Constants.RELATED_PERSON, "_id=", memberId));
            for (Bundle.BundleEntryComponent relatedPersonEntry : relatedPersonBundle.getEntry()) {
                RelatedPerson relatedPerson = (RelatedPerson) relatedPersonEntry.getResource();
                patientId = relatedPerson.getIdentifier().stream().filter(identifier -> StringUtil.concatString(BASE_IDENTIFIER, Constants.PATIENT_ID).equals(identifier.getSystem())).map(Identifier::getValue).findFirst().orElse(null);
            }
            if (!Objects.isNull(currentStatus)) {
                try (PreparedStatement pstmt = connection.prepareStatement(Constants.INSERT_INTO_PATIENT_STATUS)) {
                    pstmt.setString(1, memberId); // Assuming this returns an integer
                    pstmt.setString(2, currentStatus);  // If it's a string, it's automatically quoted
                    pstmt.setString(3, encounterType);
                    pstmt.setString(4, category);
                    pstmt.setString(5, ticketType);
                    pstmt.setString(6, reason);
                    pstmt.setString(7, patientId);

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