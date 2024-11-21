package com.mdtlabs.migration.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.ProvenanceDTO;

/**
 * <p>
 * Utility class for FHIR-related operations.
 * Provides methods for interacting with FHIR resources, including creating clients,
 * parsing and validating resources, and managing FHIR bundles.
 * </p>
 *
 * @author Nandhakumar Karthikeyan created on Feb 09, 2024
 */
public class FhirUtils {

    public static String USER;
    public static Long SPICE_USER;
    private static String SYSTEM_CODE = "http://terminology.hl7.org/CodeSystem/contractsignertypecodes";

    /**
     * Sets a bundle with a resource, full URL, HTTP verb, and provenance information.
     * This method is used to add a new entry to a FHIR Bundle with the specified parameters,
     * including creating and adding a Provenance resource based on the operation performed.
     *
     * @param url        The URL of the resource within the FHIR server.
     * @param fullUrl    The full URL of the resource.
     * @param verb       The HTTP verb used for the request (e.g., POST, PUT).
     * @param resource   The FHIR resource to be added to the bundle.
     * @param bundle     The FHIR Bundle object to which the resource will be added.
     * @param provenance The provenance information related to the operation.
     */
    public static void setBundle(String url,
                                 String fullUrl,
                                 Bundle.HTTPVerb verb,
                                 Resource resource,
                                 Bundle bundle,
                                 ProvenanceDTO provenance) {
        setCreatedBy(url, bundle, provenance, verb, resource.getMeta().getVersionId());
        bundle.addEntry().setFullUrl(fullUrl).setResource(resource).getRequest().setMethod(verb).setUrl(url);
    }

    /**
     * Creates and adds a Provenance resource to a FHIR Bundle.
     * This method constructs a Provenance resource for a given operation, identified by the HTTP verb,
     * and adds it to the specified FHIR Bundle. The Provenance resource includes details such as the
     * operation's target, the acting agent, and the operation's role, based on the provided parameters.
     *
     * @param url               The URL of the resource the Provenance is associated with.
     * @param bundle            The FHIR Bundle object to which the Provenance resource is being added.
     * @param provenanceDetails The details of the provenance, including modified date, organization ID, and user ID.
     * @param verb              The HTTP verb (e.g., POST, PUT) indicating the action performed on the resource.
     */
    public static void setCreatedBy(String url, Bundle bundle, ProvenanceDTO provenanceDetails, Bundle.HTTPVerb verb, String versionId) {
        if (bundle == null || provenanceDetails == null) {
            throw new IllegalArgumentException(Constants.LOG);
        }

        Provenance provenance = new Provenance();
        String uuid = getUniqueId();
        versionId = Objects.isNull(versionId) ? Constants.STRING_ZERO : versionId;

        List<Coding> coding = new ArrayList<>();
        if (verb.equals(Bundle.HTTPVerb.PUT)) {
            coding.add(new Coding()
                    .setCode(Constants.AMENDER).setDisplay(Constants.EMPTY).setSystem(SYSTEM_CODE));
        } else {
            coding.add(new Coding()
                    .setCode(Constants.TRANS).setDisplay(Constants.EMPTY).setSystem(SYSTEM_CODE));
        }

        provenance.getActivity().setText(versionId);
        provenance.setRecorded(provenanceDetails.getModifiedDate());
        provenance.addTarget(new Reference(url));
        provenance.addAgent().setWho(new Reference(StringUtil.concatString(String.valueOf(ResourceType.Practitioner),
                        Constants.FORWARD_SLASH, provenanceDetails.getUserId()))).setRole(List.of(new CodeableConcept().setCoding(coding).setText(verb.toString())));
        bundle.addEntry()
                .setFullUrl(StringUtil.concatString(Constants.FHIR_BASE_URL, uuid))
                .setResource(provenance)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setUrl(StringUtil.concatString(String.valueOf(ResourceType.Provenance), Constants.FORWARD_SLASH,
                        Constants.FHIR_BASE_URL, uuid));
    }

    /**
     * Generates a unique identifier using UUID.
     * <p>
     * This method leverages the {@link UUID#randomUUID()} method to generate a universally unique identifier (UUID).
     * The generated UUID is returned as a string and can be used as a unique identifier for resources or entities
     * within the application.
     * </p>
     *
     * @return A string representation of a unique UUID.
     */
    public static String getUniqueId() {
        return UUID.randomUUID().toString();
    }

    public static ProvenanceDTO getProvenance() {
        ProvenanceDTO provenance = new ProvenanceDTO();
        provenance.setSpiceUserId(SPICE_USER);
        provenance.setUserId(USER);
        provenance.setOrganizationId(Constants.STRING_ZERO);
        provenance.setModifiedDate(new Date());
        return provenance;
    }
}
