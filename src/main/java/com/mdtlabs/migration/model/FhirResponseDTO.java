package com.mdtlabs.migration.model;

import java.util.List;

import lombok.Data;

@Data
public class FhirResponseDTO {
    
    private String id;

    private String resourceType;

    private List<Object> entry;
}
