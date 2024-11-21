package com.mdtlabs.migration.script;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.RelatedPerson;

import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.connection.SqlConnection;
import com.mdtlabs.migration.util.StringUtil;
import com.mdtlabs.migration.util.RestUtil;

public class HouseholdSequence {

    private Connection connection =
            SqlConnection.getSqlConnection().getConnection();

    public static String URL;
    public static String BASE_IDENTIFIER;

    private String IDENTIFIER = "identifier=" + BASE_IDENTIFIER;
    private String PATIENT_IDENTIFIER = BASE_IDENTIFIER + "patient-id";
    private String HOUSEHOLD_IDENTIFIER = BASE_IDENTIFIER + "household-number";
    private String HOUSEHOLD_ID_IDENTIFIER = BASE_IDENTIFIER + "household-id";

    public void updateMemberSequence() throws SQLException {
        Set<Integer> villageIds = new HashSet<>();
        RestUtil restUtil = new RestUtil();
        Map<Integer, Integer> villageAndCount = new HashMap<>();
        Statement statement = connection.createStatement();

        // Fetch village IDs
        ResultSet resultSet = statement.executeQuery(Constants.VILLAGE_QUERY);
        while (resultSet.next()) {
            villageIds.add(resultSet.getInt(Constants.ID));
        }

        // Iterate through each village ID
        for (int id : villageIds) {
            List<String> patientIds = new ArrayList<>();  // Reset the list for each village

            // Fetch FHIR data
            Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL,
                    Constants.RELATED_PERSON, IDENTIFIER, Constants.VILLAGE_ID, Constants.VERTICAL_BAR) + id + "&identifier=" + HOUSEHOLD_ID_IDENTIFIER + Constants.VERTICAL_BAR);

            // Loop through each entry in the bundle and extract household member IDs
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                RelatedPerson relatedPerson = (RelatedPerson) entry.getResource();
                for (Identifier identifier : relatedPerson.getIdentifier()) {
                    if (PATIENT_IDENTIFIER.equals(identifier.getSystem())) {
                        String memberId = identifier.getValue();

                        // Extract the last 5 digits from the memberId
                        String lastFourDigits = memberId.length() > 4
                                ? memberId.substring(memberId.length() - 4)
                                : memberId;  // If it's less than 5 digits, use the full string

                        // Trim leading zeros
                        String trimmedDigits = lastFourDigits.replaceFirst("^0+(?!$)", "");  // Removes leading zeros

                        // Add the trimmed value to patientIds
                        patientIds.add(trimmedDigits);
                    }
                }
            }

            // Check if patientIds is empty
            if (!patientIds.isEmpty()) {
                // Convert to integers and find the maximum household ID
                int maxId = patientIds.stream()
                        .mapToInt(Integer::parseInt)
                        .max()
                        .orElse(0);  // Default to 0 if the list is empty
                villageAndCount.put(id, maxId);
            } else {
                // Handle empty list case, e.g., set default maxId as 0
                villageAndCount.put(id, 0);
            }
        }

        // Update the village table with the max household member IDs
        for (Map.Entry<Integer, Integer> entry : villageAndCount.entrySet()) {
            int villageId = entry.getKey();
            int memberCount = entry.getValue();

            // Build and execute the update query
            String updateQuery = String.format(Constants.MEMBER_SEQUENCE, memberCount, villageId);
            statement.executeUpdate(updateQuery);
        }

        // Close the statement
        statement.close();
    }

    //Updates village household sequence based on group the highest member id
    public void updateHouseholdSequence() throws SQLException {
        Set<Integer> villageIds = new HashSet<>();
        RestUtil restUtil = new RestUtil();
        Map<Integer, Integer> villageAndCount = new HashMap<>();
        Statement statement = connection.createStatement();

        // Fetch village IDs
        ResultSet resultSet = statement.executeQuery(Constants.VILLAGE_QUERY);
        while (resultSet.next()) {
            villageIds.add(resultSet.getInt("id"));
        }

        // Iterate through each village ID
        for (int id : villageIds) {
            List<String> householdMemberIds = new ArrayList<>();  // Reset the list for each village

            // Fetch FHIR data
            Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL,
                    Constants.GROUP, IDENTIFIER, Constants.VILLAGE_ID, Constants.VERTICAL_BAR) + id);

            // Loop through each entry in the bundle and extract household member IDs
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Group group = (Group) entry.getResource();
                for (Identifier identifier : group.getIdentifier()) {
                    if (HOUSEHOLD_IDENTIFIER.equals(identifier.getSystem())) {
                        String memberId = identifier.getValue();
                        householdMemberIds.add(memberId);
                    }
                }
            }

            // Check if householdMemberIds is empty
            if (!householdMemberIds.isEmpty()) {
                // Convert to integers and find the maximum household ID
                int maxId = householdMemberIds.stream()
                        .mapToInt(Integer::parseInt)
                        .max()
                        .orElse(0);  // Default to 0 if the list is empty
                villageAndCount.put(id, maxId);
            } else {
                // Handle empty list case, e.g., set default maxId as 0
                villageAndCount.put(id, 0);
            }
        }

        // Update the village table with the max household member IDs
        for (Map.Entry<Integer, Integer> entry : villageAndCount.entrySet()) {
            int villageId = entry.getKey();
            int memberCount = entry.getValue();

            // Build and execute the update query
            String updateQuery = String.format(Constants.HOUSEHOLD_SEQUENCE, memberCount, villageId);
            statement.executeUpdate(updateQuery);
        }

        // Close the statement
        statement.close();
    }

}
