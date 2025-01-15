package com.mdtlabs.migration.util;

import java.lang.*;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.model.FhirResponseDTO;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class RestUtil {

    public static String URL;
    public static Integer THREADSIZE;
    public static int BUNDLE_SIZE;
    public static String CLIENT;
    public static String TOKEN;

    public RestUtil() {

    }

    public void saveBundle(Bundle bundle) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity request = constructRequestEntity(bundle);
        restTemplate.postForEntity(URL, request, FhirResponseDTO.class);
    }

    public HttpEntity constructRequestEntity(Bundle bundle) {
        FhirContext fhirContext = FhirContext.forR4();
        IParser parser = fhirContext.newJsonParser();
        String bundleDto = parser.encodeResourceToString(bundle);
        System.out.println("values ----------->" + bundleDto);
        HttpHeaders headers = new HttpHeaders();
        headers.set(Constants.CLIENT, CLIENT);
        headers.set(Constants.AUTHORIZATION, TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(bundleDto, headers);
    }

    public Bundle getDataFromFhir(String queryUrl) {
        System.out.println("Query Url ----------------------------------------------------");
        System.out.println(queryUrl);
        System.out.println("Query Url ----------------------------------------------------");

        RestTemplate restTemplate = new RestTemplate();
        FhirContext ctx = FhirContext.forR4();
        Bundle mergedBundle = new Bundle();
        Bundle bundle;
        long batchSize = 1000L;
        long pageOffset = 0L;
        do {
            HttpEntity<String> res = restTemplate.exchange(
                    queryUrl + String.format(Constants.COUNT, batchSize, pageOffset), HttpMethod.GET,
                    constructRequestEntityWithoutBundle(), String.class);
            // Parse the JSON string
            bundle = ctx.newJsonParser().parseResource(Bundle.class, res.getBody());
            // Add each entry from the retrieved bundle to the merged bundle
            bundle.getEntry().forEach(mergedBundle::addEntry);
            pageOffset += batchSize;
        } while (bundle.hasEntry());
        return mergedBundle;
    }

    public Bundle getBatchRequest(String url) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, constructRequestEntityWithoutBundle(),
                String.class);
        FhirContext ctx = FhirContext.forR4();
        // Parse the JSON string
        return ctx.newJsonParser().parseResource(Bundle.class, res.getBody());
    }

    public HttpEntity constructRequestEntityWithoutBundle() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(Constants.CLIENT, CLIENT);
        headers.set(Constants.AUTHORIZATION, TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(headers);
    }

    public void saveBundleInBatches(Bundle bundle, int batchSize) {
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        int totalResources = entries.size();
        for (int resouceIndex = 0; resouceIndex < totalResources; resouceIndex += batchSize) {
            int toIndex = Math.min(resouceIndex + batchSize, totalResources);
            Bundle batchBundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
            List<Bundle.BundleEntryComponent> batchEntries = entries.subList(resouceIndex, toIndex);

            batchBundle.setEntry(batchEntries);

            saveBatchBundle(batchBundle);
        }
    }

    public void saveBatchBundle(Bundle updatedBundle) {
        RestUtil restUtil = new RestUtil();
        if (!updatedBundle.getEntry().isEmpty()) {
            System.out.println("saved");
            restUtil.saveBundle(updatedBundle);
        }
    }
}
