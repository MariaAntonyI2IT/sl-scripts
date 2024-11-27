package com.mdtlabs.migration.model;

public class Constants {

    public static final String HOUSEHOLD_ID = "household-id";

    private Constants() {}

    public static final String RELATED_PERSON = "RelatedPerson?";
    public static final String PATIENT = "Patient?";
    public static final String GROUP = "Group?";
    public static final String CLIENT = "client";
    public static final String AUTHORIZATION = "Authorization";
    public static final String COUNT = "&_count=%s&_getpagesoffset=%s&_sort=_id";
    public static final String GET_MEMBER_ID = "Group?_id=%s";
    public static final String DEVICE_LIST = "Device?_id=%s";
    public static final String PHONE_CATEGORY = "phone-category";
    public static final String PHONE_NUMBER = "phone";
    // Common Symbols & Strings
    public static final String SPACE = " ";
    public static final String EMPTY = "";
    public static final String FORWARD_SLASH = "/";
    public static final String VERTICAL_BAR = "|";
    public static final String STRING_ZERO = "0";
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final String DATE_FORMAT_DD_MM_YYYY = "dd/MM/yyyy";

    // Spouse & household sequence
    public static final String SPOUSE_PARTNER = "Spouse / Partner";
    public static final String WIFE_HUSBAND = "Wife / Husband";
    public static final String LOG = "Bundle and provenanceDetails cannot be null";
    public static final String AMENDER = "AMENDER";
    public static final String TRANS = "TRANS";
    public static final String FHIR_BASE_URL = "urn:uuid:";
    public static final String PROVENANCE = "Provenance";
    public static final String VILLAGE_QUERY = "SELECT id FROM village ORDER BY id";
    public static final String VILLAGE_OBJECT_QUERY = "select v.id as id, member_sequence as member_sequence, c.code as ccode  from village v  join chiefdom c on c.id = v.chiefdom_id ORDER BY v.id";

    public static final String ID = "id";
    public static final String VILLAGE_ID = "village-id";
    public static final String MEMBER_SEQUENCE = "UPDATE village SET member_sequence = %d WHERE id = %d";
    public static final String HOUSEHOLD_SEQUENCE = "UPDATE village SET household_sequence = %d WHERE id = %d";
    public static final String UNASSIGNED = "Unassigned";
    public static final String PATIENT_ID = "patient-id";

    // Diagnosis
    public static final String MALARIA = "malaria";
    public static final String ABOVE_FIVE_YEARS = "ABOVE_FIVE_YEARS";
    public static final String ANC_REVIEW = "ANC_REVIEW";
    public static final String UNDER_TWO_MONTHS = "UNDER_TWO_MONTHS";
    public static final String PNC_MOTHER_REVIEW = "PNC_MOTHER_REVIEW";
    public static final String UNDER_FIVE_YEARS = "UNDER_FIVE_YEARS";
    public static final String UNCOMPLICATED_MALARIA = "uncomplicatedMalaria";

    // Facility Report Admin
    public static final String FACILITY_REPORT_ADMIN_QUERY = "select u.id, uo.organization_id from \"user\" u inner join user_role ur on u.id = ur.user_id inner join role r on ur.role_id = r.id inner join user_organization uo on u.id = uo.user_id where r.name = 'FACILITY_REPORT_ADMIN'";
    public static final String ORGANIZATION_ID = "organization_id";
    public static final String INSERT_INTO_REPORT_USER_ORGANIZATION = "INSERT INTO report_user_organization (user_id, organization_id) VALUES (%d, %d)";
    public static final String CREATED_BY = "created_by";
    public static final String UPDATED_BY = "updated_by";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String IS_ACTIVE = "is_active";
    public static final String IS_DELETED = "is_deleted";
    public static final String SELECT_QUERY = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = ?)";
    public static final String VARCHAR_ARRAY = "varchar[]";
    public static final String VARCHAR = "varchar";
    public static final String INT = "int";
    public static final String DOUBLE = "double";
    public static final String BOOLEAN = "boolean";
    public static final String JSONB = "jsonb";
    public static final String TABLES = "tables";
    public static final String NAME = "name";
    public static final String COLUMNS = "columns";
    public static final String TYPE = "type";
    public static final int BATCH_SIZE = 1000;
    public static final String RELATIONSHIP = "relationships";
    public static final String TARGET_TABLE = "target_table";
    public static final String FOREIGN_KEY = "foreign_key";
}
