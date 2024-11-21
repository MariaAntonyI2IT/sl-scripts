package com.mdtlabs.migration.script;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.ProvenanceDTO;
import com.mdtlabs.migration.util.FhirUtils;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;
import org.hl7.fhir.r4.model.*;

import java.util.Objects;


public class HouseholdNumberTypeScript {
    public static String URL;
    public static String BASE_IDENTIFIER;

    public void updateHouseholdNumberType() {
        RestUtil restUtil = new RestUtil();
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL, Constants.RELATED_PERSON, "relationship:code=HouseholdHead"));
        Bundle updatedBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        ProvenanceDTO provenance = FhirUtils.getProvenance();
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            RelatedPerson relatedPerson = (RelatedPerson) entry.getResource();
            String householdId = null;
            for (Identifier identifier : relatedPerson.getIdentifier()) {
                if (StringUtil.concatString(BASE_IDENTIFIER, Constants.HOUSEHOLD_ID).equals(identifier.getSystem()) || Constants.HOUSEHOLD_ID.equals(identifier.getSystem())) {
                    householdId = identifier.getValue();
                }
            }
            if (!Objects.isNull(householdId)) {
                ContactPoint.ContactPointUse contactUse = relatedPerson.getTelecom().get(0).getUse();
                Bundle groupBundle = restUtil.getBatchRequest(String.format((StringUtil.concatString(URL, Constants.GET_MEMBER_ID)), householdId));
                for (Bundle.BundleEntryComponent groupEntry : groupBundle.getEntry()) {
                    Group group = (Group) groupEntry.getResource();
                    group.getMember().stream().forEach(groupMemberComponent -> {
                        String valueReference = groupMemberComponent.getEntity().getReference();
                        if (valueReference.startsWith(String.valueOf(ResourceType.Device))) {
                            String deviceId = getIdFromReference(valueReference);
                            Bundle deviceBundle = restUtil.getBatchRequest(String.format((StringUtil.concatString(URL, Constants.DEVICE_LIST)), deviceId));
                            Boolean categoryPresent = Boolean.FALSE;
                            Boolean phoneURL = Boolean.FALSE;
                            for (Bundle.BundleEntryComponent deviceEntry : deviceBundle.getEntry()) {
                                Device device = (Device) deviceEntry.getResource();
                                for (Identifier identifier : device.getIdentifier()) {
                                    if (StringUtil.concatString(BASE_IDENTIFIER, Constants.PHONE_CATEGORY).equals(identifier.getSystem())) {
                                        categoryPresent = Boolean.TRUE;
                                    }
                                    if (Constants.PHONE_NUMBER.equals(identifier.getSystem())) {
                                        identifier.setSystem(StringUtil.concatString(BASE_IDENTIFIER, Constants.PHONE_NUMBER));
                                        phoneURL = Boolean.TRUE;
                                    }
                                }
                                if (categoryPresent.equals(Boolean.FALSE)) {
                                    device.addIdentifier().setSystem(StringUtil.concatString(BASE_IDENTIFIER, Constants.PHONE_CATEGORY))
                                            .setValue(contactUse.toString().toLowerCase());
                                }
                                if (categoryPresent.equals(Boolean.FALSE) || phoneURL.equals(Boolean.TRUE)) {
                                    FhirUtils.setBundle(StringUtil.concatString(String.valueOf(ResourceType.Device), Constants.FORWARD_SLASH, deviceId),
                                            StringUtil.concatString(String.valueOf(ResourceType.Device), deviceId),
                                            Bundle.HTTPVerb.PUT, device, updatedBundle, provenance);
                                }
                            }
                        }
                    });
                    System.out.println("household"+householdId);
                }
            }
        }
        FhirContext fhirContext = FhirContext.forR4();
        IParser parser = fhirContext.newJsonParser();
        String bundleDto = parser.encodeResourceToString(updatedBundle);
        System.out.println(bundleDto);
        if (!updatedBundle.getEntry().isEmpty()) {
            System.out.println("saved");
            restUtil.saveBundle(updatedBundle);
        }
    }

    public String getIdFromReference(String reference) {
        String[] referencePaths = reference.split(Constants.FORWARD_SLASH);
        return referencePaths[referencePaths.length - Constants.ONE];
    }

}
