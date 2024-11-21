package com.mdtlabs.migration.script;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.hl7.fhir.r4.model.ResourceType;

import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.ProvenanceDTO;
import com.mdtlabs.migration.util.FhirUtils;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;

public class SpousePartner {

    public static String URL;

    //Updates Related Person relationship spouse / partner to wife / husband
    public void updateSpouseData() {
        RestUtil restUtil = new RestUtil();
        Bundle updatedBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        // Gets data from fhir
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL,Constants.RELATED_PERSON));
        // Create a flag to track if any updates were made
        boolean isUpdated = false;  // Fix: initially, no updates made

        ProvenanceDTO provenance = FhirUtils.getProvenance();

        // Loop through each entry in the bundle and filter for "Spouse / Partner"
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            RelatedPerson relatedPerson = (RelatedPerson) entry.getResource();
            // Check if the relationship is "Spouse / Partner" and update it
            if (relatedPerson.hasRelationship()) {
                for (CodeableConcept relationship : relatedPerson.getRelationship()) {
                    if (Constants.SPOUSE_PARTNER.equals(relationship.getText())) {
                        relationship.setText(Constants.WIFE_HUSBAND);
                        relationship.getCodingFirstRep().setCode(Constants.WIFE_HUSBAND);
                        FhirUtils.setBundle(StringUtil.concatString(String.valueOf(ResourceType.RelatedPerson), Constants.FORWARD_SLASH, relatedPerson.getIdPart()),
                                StringUtil.concatString(String.valueOf(ResourceType.RelatedPerson), relatedPerson.getIdPart()),
                                Bundle.HTTPVerb.PUT, relatedPerson, updatedBundle, provenance);
                        // Mark as updated
                        isUpdated = true;
                    }
                }
            }
        }

        // Save the updated bundle only if updates were made
        if (isUpdated) {
            restUtil.saveBundle(updatedBundle);  // Use the updated bundle for saving
        }
    }
}
