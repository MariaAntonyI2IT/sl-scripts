package com.mdtlabs.migration.script;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.ProvenanceDTO;
import com.mdtlabs.migration.util.FhirUtils;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;
import org.hl7.fhir.r4.model.*;

public class EncounterUpdate {
    public static String URL;
    public static String BASE_IDENTIFIER;

    private String IDENTIFIER = "identifier=";
    private String PRESCRIPTION_STATUS = BASE_IDENTIFIER+"prescription-status";//chnage in dev
    private String INVESTIGATION_STATUS = BASE_IDENTIFIER+"investigation-status";
    public static int BATCH_SIZE;

    public void updateLocationInEncounter() {
        Bundle updatedBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        ProvenanceDTO provenance = FhirUtils.getProvenance();
        RestUtil restUtil = new RestUtil();
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL,
                Constants.ENCOUNTER, IDENTIFIER, PRESCRIPTION_STATUS, Constants.COMMA, INVESTIGATION_STATUS, Constants.VERTICAL_BAR));
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            boolean identifier_check = Boolean.FALSE;
            Encounter encounter = (Encounter) entry.getResource();
            for (Identifier identifier : encounter.getIdentifier()) {
                if (!(identifier.getSystem().equals(INVESTIGATION_STATUS)) && !identifier.getSystem().equals(PRESCRIPTION_STATUS)) {
                    identifier_check = Boolean.TRUE;
                    break;
                }
            }
            if (Boolean.FALSE.equals(identifier_check) && encounter.getLocation().isEmpty()) {
                Encounter.EncounterLocationComponent component = new Encounter.EncounterLocationComponent();
                Location location = new Location();
                location.getPosition().setLatitude(0.0);
                location.getPosition().setLongitude(0.0);
                component.setLocation(new Reference(location));
                encounter.addLocation(component);
                FhirUtils.setBundle(StringUtil.concatString(String.valueOf(ResourceType.Encounter), Constants.FORWARD_SLASH, encounter.getIdPart()),
                        StringUtil.concatString(String.valueOf(ResourceType.Encounter), encounter.getIdPart()),
                        Bundle.HTTPVerb.PUT, encounter, updatedBundle, provenance);
            }
        }
        restUtil.saveBundleInBatches(updatedBundle,BATCH_SIZE);
    }

}
