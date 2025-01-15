package com.mdtlabs.migration.script;

import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.ProvenanceDTO;
import com.mdtlabs.migration.util.FhirUtils;
import com.mdtlabs.migration.util.RestUtil;
import com.mdtlabs.migration.util.StringUtil;
import org.hl7.fhir.r4.model.*;

import java.util.Arrays;

public class MemberLocationUpdate {
    public static String URL;
    public static int BATCH_SIZE;

    public void locatioUpdate() {
        Bundle updatedBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        ProvenanceDTO provenance = FhirUtils.getProvenance();
        RestUtil restUtil = new RestUtil();
        Bundle bundle = restUtil.getDataFromFhir(StringUtil.concatString(URL,
                Constants.RELATED_PERSON));
        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            RelatedPerson relatedPerson = (RelatedPerson) entry.getResource();
            Extension extension = new Extension();
            extension.setUrl(Constants.GEOLOCATIONURL);
            Extension latitudeExtension=new Extension();
            latitudeExtension.setUrl(Constants.LATITUDE);
            latitudeExtension.setValue(new DecimalType(0.0));
            Extension longitudeExtention=new Extension();
            longitudeExtention.setUrl(Constants.LONGITUDE);
            longitudeExtention.setValue(new DecimalType(0.0));
            extension.setExtension(Arrays.asList(latitudeExtension,longitudeExtention));
            relatedPerson.addExtension(extension);
            FhirUtils.setBundle(StringUtil.concatString(String.valueOf(ResourceType.RelatedPerson), Constants.FORWARD_SLASH, relatedPerson.getIdPart()),
                    StringUtil.concatString(String.valueOf(ResourceType.RelatedPerson), relatedPerson.getIdPart()),
                    Bundle.HTTPVerb.PUT, relatedPerson, updatedBundle, provenance);
        }
        restUtil.saveBundleInBatches(updatedBundle,BATCH_SIZE);
    }


}
