package com.mdtlabs.migration.model;

import java.util.Date;

import lombok.Data;

@Data
public class ProvenanceDTO {

    private String userId;

    private Long spiceUserId;

    private String organizationId;

    private Date modifiedDate;
}
