package com.mdtlabs.migration.script;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.mdtlabs.migration.connection.SqlConnection;
import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.ProvenanceDTO;
import com.mdtlabs.migration.util.FhirUtils;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.ResourceType;

public class PatientIdUpdate {
    private static final Map<String, String> patientIdsmap = new HashMap<>();
    private static final Map<String, Map<String, String>> csvDetails = new HashMap<>();
    private static final Map<Integer, Map<String, String>> villageMap = new HashMap<>();
    private static final Map<String, String> motherRelationIds = new HashMap<>();
    public static String URL;
    public static String BASE_IDENTIFIER;
    private final Connection connection = SqlConnection.getSqlConnection().getConnection();
    public String IDENTIFIER = "identifier=" + BASE_IDENTIFIER;
    public String PATIENT_IDENTIFIER = BASE_IDENTIFIER + "patient-id";
    public String PATIENT_IDENTIFIER_ID_OLD = BASE_IDENTIFIER + "old-patient-id";
    public String MOTHER_PATIENT_ID_IDENTIFIER = BASE_IDENTIFIER + "mother-patient-id";
    public String HOUSEHOLD_ID_IDENTIFIER = BASE_IDENTIFIER + "household-id";
    RestUtil restUtil = new RestUtil();

    public void updateMemberSequence() throws SQLException {
        Set<Integer> villageIds = new HashSet<>();
        Statement statement = connection.createStatement();
        ProvenanceDTO provenance = FhirUtils.getProvenance();


        // Fetch village IDs
        ResultSet resultSet = statement.executeQuery(Constants.VILLAGE_OBJECT_QUERY);
        while (resultSet.next()) {
            villageIds.add(resultSet.getInt(Constants.ID));
            villageMap.put(resultSet.getInt(Constants.ID),
                    Map.of("code", resultSet.getString("code"), "sequence", resultSet.getString("member_sequence"),
                            //member_sequence
                            Constants.ID, String.valueOf(resultSet.getInt(Constants.ID))));
        }

        // Iterate through each village ID
        for (int id : villageMap.keySet()) {

            List<String> relatedPersonsIds = new ArrayList<>();
            List<String> patientIds = new ArrayList<>();  // Reset the list for each village
            Bundle updatedBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
            int maxsequence = Integer.parseInt(villageMap.get(id).get("sequence"));  // set village max sequence number
            String villageCode = villageMap.get(id).get("code");// set village code value

            if (villageCode.length() == 1) {
                villageCode = "000" + villageCode;
            } else if (villageCode.length() == 2) {
                villageCode = "00" + villageCode;
            } else if (villageCode.length() == 3) {
                villageCode = "0" + villageCode;
            }

            // Fetch FHIR data get related person without mother patient ids
            Bundle bundle = restUtil.getDataFromFhir(
                    StringUtil.concatString(URL, Constants.RELATED_PERSON, IDENTIFIER, Constants.VILLAGE_ID,
                            Constants.VERTICAL_BAR) + id + "&_count=99999&identifier:not=" +
                            MOTHER_PATIENT_ID_IDENTIFIER + "|&identifier:not=" + HOUSEHOLD_ID_IDENTIFIER + "|");

            System.out.println("Result Query First value " + bundle.getEntry().size());
//            FhirContext fhirContext = FhirContext.forR4();
//            IParser parser = fhirContext.newJsonParser();
//            String bundleDto = parser.encodeResourceToString(bundle);
//            System.out.println("values ----------->" + bundleDto);

            // Loop through each entry in the bundle and extract household member IDs and without mother patient ids
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                RelatedPerson relatedPerson = (RelatedPerson) entry.getResource();
                String oldPatientIdValue = null;

                for (Identifier identifier : relatedPerson.getIdentifier()) {
                    if (PATIENT_IDENTIFIER.equals(identifier.getSystem()) && !Objects.isNull(identifier.getValue())) {
                        oldPatientIdValue = identifier.getValue();
                        String newPatientId = generatePatientId(oldPatientIdValue, maxsequence, villageCode);
                        maxsequence = maxsequence + 1;
                        identifier.setValue(newPatientId);
                        patientIdsmap.put(oldPatientIdValue, newPatientId);
                        csvDetails.put(relatedPerson.getIdPart(),
                                Map.of("oldpatientId", oldPatientIdValue, "newpatientId", newPatientId, "villageCode",
                                        villageCode, "villageId", String.valueOf(id)));
                    }
                }
               // relatedPerson.addIdentifier().setSystem(PATIENT_IDENTIFIER_ID_OLD).setValue(oldPatientIdValue);
                relatedPersonsIds.add(relatedPerson.getIdPart());
                FhirUtils.setBundle(
                        StringUtil.concatString(String.valueOf(ResourceType.RelatedPerson), Constants.FORWARD_SLASH,
                                relatedPerson.getIdPart()),
                        StringUtil.concatString(String.valueOf(ResourceType.RelatedPerson), relatedPerson.getIdPart()),
                        Bundle.HTTPVerb.PUT, relatedPerson, updatedBundle, provenance);
            }

            // get with mother patient related persons
            Bundle bundleWithMotherPatinetId = restUtil.getDataFromFhir(
                    StringUtil.concatString(URL, Constants.RELATED_PERSON, IDENTIFIER, Constants.VILLAGE_ID,
                            Constants.VERTICAL_BAR) + id + "&identifier=" + MOTHER_PATIENT_ID_IDENTIFIER +
                            "|&_count=99999&identifier:not=" + HOUSEHOLD_ID_IDENTIFIER + "|");

            System.out.println("Result Query value " + bundleWithMotherPatinetId.getEntry().size());

            // Loop through each entry in the bundle and extract household member IDs and without mother patientids
            for (Bundle.BundleEntryComponent entry : bundleWithMotherPatinetId.getEntry()) {
                RelatedPerson relatedPerson = (RelatedPerson) entry.getResource();
                String oldPatientIdValue = null;

                for (Identifier identifier : relatedPerson.getIdentifier()) {
                    if (PATIENT_IDENTIFIER.equals(identifier.getSystem()) && !Objects.isNull(identifier.getValue())) {
                        oldPatientIdValue = identifier.getValue();
                        String newPatientId = generatePatientId(oldPatientIdValue, maxsequence, villageCode);
                        maxsequence = maxsequence + 1;
                        identifier.setValue(newPatientId);
                        patientIdsmap.put(oldPatientIdValue, newPatientId);
                        csvDetails.put(relatedPerson.getIdPart(),
                                Map.of("oldpatientId", oldPatientIdValue, "newpatientId", newPatientId, "villageCode",
                                        villageCode, "villageId", String.valueOf(id)));
                    }

                    if (MOTHER_PATIENT_ID_IDENTIFIER.equals(identifier.getSystem()) &&
                            !Objects.isNull(identifier.getValue())) {
                        identifier.setValue(patientIdsmap.get(identifier.getValue()));
                    }
                }
               // relatedPerson.addIdentifier().setSystem(PATIENT_IDENTIFIER_ID_OLD).setValue(oldPatientIdValue);
                relatedPersonsIds.add(relatedPerson.getIdPart());
                FhirUtils.setBundle(
                        StringUtil.concatString(String.valueOf(ResourceType.RelatedPerson), Constants.FORWARD_SLASH,
                                relatedPerson.getIdPart()),
                        StringUtil.concatString(String.valueOf(ResourceType.RelatedPerson), relatedPerson.getIdPart()),
                        Bundle.HTTPVerb.PUT, relatedPerson, updatedBundle, provenance);
            }
            if (!updatedBundle.getEntry().isEmpty()) {
                updatePatients(relatedPersonsIds, provenance, updatedBundle);
                updateVitals(relatedPersonsIds, provenance, updatedBundle);
                restUtil.saveBundle(updatedBundle);  // Use the updated bundle for saving
            }

            // Build and execute the update query
            String updateQuery = String.format(Constants.MEMBER_SEQUENCE, maxsequence, id);
            statement.executeUpdate(updateQuery);
            System.out.println("COMPLETED");
        }
        System.out.println("Result: ");
        String csvFile = "output.csv";
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            // Write the header
            writer.println("villageId, villageCode, memberId, oldPatientId, NewPatientId");

            for (String idValue : csvDetails.keySet()) {
                writer.println(StringUtil.concatString(csvDetails.get(idValue).get("villageId"), ",",
                        csvDetails.get(idValue).get("villageCode"), ",", idValue, ",",
                        csvDetails.get(idValue).get("oldpatientId"), ",", csvDetails.get(idValue).get("newpatientId")));
                // System.out.println("oldPatientId: " + idValue + "  |  newPatientId: " + patientIdsmap.get(idValue));
            }

        } catch (Exception e) {
            System.out.println("Error while writing to the CSV file: " + e.getMessage());
        }

        // Close the statement
        statement.close();
    }

    // update patient id in patient resources
    private void updatePatients(List<String> ids, ProvenanceDTO provenance, Bundle updatedBundle) {
        Bundle bundleWithMotherPatinetId = restUtil.getDataFromFhir(
                StringUtil.concatString(URL, Constants.PATIENT, "link=", String.join(",", ids)));

        // Loop through each entry in the bundle and extract household member IDs and without mother patientids
        for (Bundle.BundleEntryComponent entry : bundleWithMotherPatinetId.getEntry()) {
            Patient patient = (Patient) entry.getResource();
            String oldPatientIdValue = null;

            for (Identifier identifier : patient.getIdentifier()) {
                if (PATIENT_IDENTIFIER.equals(identifier.getSystem()) && !Objects.isNull(identifier.getValue())) {
                    oldPatientIdValue = identifier.getValue();
                    identifier.setValue(patientIdsmap.get(identifier.getValue()));
                }
                if (MOTHER_PATIENT_ID_IDENTIFIER.equals(identifier.getSystem()) &&
                        !Objects.isNull(identifier.getValue())) {
                    identifier.setValue(patientIdsmap.get(identifier.getValue()));
                }
            }
           // patient.addIdentifier().setSystem(PATIENT_IDENTIFIER_ID_OLD).setValue(oldPatientIdValue);
            FhirUtils.setBundle(StringUtil.concatString(String.valueOf(ResourceType.Patient), Constants.FORWARD_SLASH,
                            patient.getIdPart()),
                    StringUtil.concatString(String.valueOf(ResourceType.Patient), patient.getIdPart()),
                    Bundle.HTTPVerb.PUT, patient, updatedBundle, provenance);
        }
    }

    // update patient neonate ids in patient vitals
    private void updateVitals(List<String> ids, ProvenanceDTO provenance, Bundle updatedBundle) {
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL,
                "Observation?identifier=PatientVitals&code:text=neonatePatientId&performer=", String.join(",", ids)));

        // Loop through each entry in the bundle and extract household member IDs and without mother patient ids
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Observation observation = (Observation) entry.getResource();
//            observation.addIdentifier().setSystem(PATIENT_IDENTIFIER_ID_OLD)
//                    .setValue(observation.getValueStringType().toString());
            observation.getValueStringType().setValue(patientIdsmap.get(observation.getValueStringType().toString()));
            //observation.getValueStringType().toString()
            FhirUtils.setBundle(
                    StringUtil.concatString(String.valueOf(ResourceType.Observation), Constants.FORWARD_SLASH,
                            observation.getIdPart()),
                    StringUtil.concatString(String.valueOf(ResourceType.Observation), observation.getIdPart()),
                    Bundle.HTTPVerb.PUT, observation, updatedBundle, provenance);
        }
    }

    // Generate new patientId based on villageCode and sequence number
    private String generatePatientId(String patientId, int maxsequence, String villageCode) {
        String chiefDomCode = patientId.substring(0, 3);
        String userId = patientId.substring(7, (patientId.length() - 4));
        DecimalFormat df = new DecimalFormat("0000");
        String formattedNumber = df.format(maxsequence);
        return StringUtil.concatString(chiefDomCode, villageCode, userId, formattedNumber);
    }

}
