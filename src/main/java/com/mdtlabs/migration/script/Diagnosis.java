package com.mdtlabs.migration.script;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.ResourceType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.ProvenanceDTO;
import com.mdtlabs.migration.util.FhirUtils;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;

public class Diagnosis {

    public static String URL;

    private static final String CONDITION_URL = "Condition?identifier=diagnosis";

    public void updateDiagnosisData() {
        RestUtil restUtil = new RestUtil();
        Bundle updatedBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        // Gets data from fhir
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL, CONDITION_URL));
        AtomicBoolean isUpdated = new AtomicBoolean(false);
        ProvenanceDTO provenance = FhirUtils.getProvenance();

        if (Objects.nonNull(bundle)) {
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            entries.stream()
                    .filter(entry -> Objects.nonNull(entry.getResource()) && entry.getResource() instanceof Condition)
                    .forEach(entry -> {
                        Condition condition = (Condition) entry.getResource();
                        CodeableConcept code = condition.getCode();

                        if (Objects.nonNull(code) && Objects.nonNull(code.getText()) && code.getText().equals(Constants.MALARIA)) {

                            List<CodeableConcept> category = condition.getCategory();
                            String diagnosis = condition.getIdentifier().get(1).getValue();

                            if (Constants.ABOVE_FIVE_YEARS.equals(diagnosis)) {
                                code.setText(Constants.UNCOMPLICATED_MALARIA);
                            } else if (Arrays.asList(Constants.ANC_REVIEW, Constants.UNDER_TWO_MONTHS, Constants.UNDER_FIVE_YEARS, Constants.PNC_MOTHER_REVIEW).contains(diagnosis)) {
                                code.setText(Constants.UNCOMPLICATED_MALARIA);

                                if (!category.isEmpty()) {
                                    category.forEach(item -> item.setText(Constants.UNCOMPLICATED_MALARIA));
                                }
                            }
                            FhirUtils.setBundle(StringUtil.concatString(String.valueOf(ResourceType.Condition), Constants.FORWARD_SLASH, condition.getIdPart()),
                                    StringUtil.concatString(Constants.FHIR_BASE_URL, condition.getIdPart()),
                                    Bundle.HTTPVerb.PUT, condition, updatedBundle, provenance);
                            // Mark as updated
                            isUpdated.set(true);
                        }
                    });
        }

        // Save the updated bundle only if updates were made
        if (isUpdated.get()) {
            restUtil.saveBundle(updatedBundle);  // Use the updated bundle for saving
        }
    }
}