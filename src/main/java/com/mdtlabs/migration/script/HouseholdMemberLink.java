package com.mdtlabs.migration.script;

import com.mdtlabs.migration.connection.SqlConnection;
import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.RelatedPerson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class HouseholdMemberLink {
    private Connection connection =
            SqlConnection.getSqlConnection().getConnection();
    public static String URL;
    public static String BASE_IDENTIFIER;
    String insertQuery = "INSERT INTO household_member_link (member_id, patient_id,village_id,name,status) VALUES (?,?,?,?,?)";

    public void householdMemberLink() throws SQLException {
        PreparedStatement statement = connection.prepareStatement(insertQuery);
        RestUtil restUtil = new RestUtil();
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL, Constants.RELATED_PERSON, "identifier:not=", BASE_IDENTIFIER, Constants.HOUSEHOLD_ID, Constants.VERTICAL_BAR));
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            RelatedPerson relatedPerson = (RelatedPerson) entry.getResource();

            String memberId = relatedPerson.getIdPart();
            String patientId = null;
            String villageId = null;
            String name = null;
            for (Identifier identifier : relatedPerson.getIdentifier()) {
                if (StringUtil.concatString(BASE_IDENTIFIER, Constants.PATIENT_ID).equals(identifier.getSystem()) || Constants.PATIENT_ID.equals(identifier.getSystem())) {
                    patientId = identifier.getValue();
                }
                if (StringUtil.concatString(BASE_IDENTIFIER, Constants.VILLAGE_ID).equals(identifier.getSystem()) || Constants.VILLAGE_ID.equals(identifier.getSystem())) {
                    villageId = identifier.getValue();
                }
            }
            if (null != relatedPerson.getName() && null != relatedPerson.getName().get(0)
                    && null != relatedPerson.getName().get(0).getText()) {
                name = (relatedPerson.getName().get(0).getText());
            }
            statement.setString(1, memberId);
            statement.setString(2, patientId);
            statement.setString(3, villageId);
            statement.setString(4, name);
            statement.setString(5, Constants.UNASSIGNED);
            int rowsAffected = statement.executeUpdate();
            System.out.println(rowsAffected + " row(s) inserted");
        }

    }
}
