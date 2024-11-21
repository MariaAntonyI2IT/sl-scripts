package com.mdtlabs.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.mdtlabs.migration.connection.FhirDatabaseConnection;
import com.mdtlabs.migration.connection.SqlConnection;
import com.mdtlabs.migration.model.Constants;
import com.mdtlabs.migration.script.Diagnosis;
import com.mdtlabs.migration.script.HouseholdMemberLink;
import com.mdtlabs.migration.script.HouseholdNumberTypeScript;
import com.mdtlabs.migration.script.HouseholdSequence;
import com.mdtlabs.migration.script.PatientIdUpdate;
import com.mdtlabs.migration.script.SpousePartner;
import com.mdtlabs.migration.util.FhirUtils;
import com.mdtlabs.migration.util.RestUtil;

public class EnvDetails {
    
    public void readProperties() {
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("config.properties")) {
            Properties prop = new Properties();
            prop.load(input);

            SqlConnection.connectionURL = prop.getProperty("db.src.url");
            SqlConnection.username = prop.getProperty("db.src.user");
            SqlConnection.password = prop.getProperty("db.src.password");

            String fhirUrl = prop.getProperty("db.fhir.url");
            if (fhirUrl != null && !fhirUrl.isEmpty()) {
                FhirDatabaseConnection.connectionURL = fhirUrl;
                FhirDatabaseConnection.username = prop.getProperty("db.fhir.user");
                FhirDatabaseConnection.password = prop.getProperty("db.fhir.password");
            }

            RestUtil.URL = prop.getProperty("url");
            RestUtil.THREADSIZE = Integer.parseInt(prop.getProperty("thread.size"));
            RestUtil.BUNDLE_SIZE = Integer.parseInt(prop.getProperty("bundle.size"));
            RestUtil.CLIENT = prop.getProperty(Constants.CLIENT);
            RestUtil.TOKEN = prop.getProperty("token");

            FhirUtils.USER = prop.getProperty("user.id");
            FhirUtils.SPICE_USER = Long.parseLong(prop.getProperty("spice.user.id"));

            HouseholdSequence.URL = prop.getProperty("url");
            SpousePartner.URL =  prop.getProperty("url");
            HouseholdMemberLink.URL = prop.getProperty("url");
            HouseholdNumberTypeScript.URL = prop.getProperty("url");
            Diagnosis.URL = prop.getProperty("url");
            PatientIdUpdate.URL = prop.getProperty("url");

            PatientIdUpdate.BASE_IDENTIFIER = prop.getProperty("identifier");
            HouseholdSequence.BASE_IDENTIFIER = prop.getProperty("identifier");
            HouseholdMemberLink.BASE_IDENTIFIER = prop.getProperty("identifier");
            HouseholdNumberTypeScript.BASE_IDENTIFIER = prop.getProperty("identifier");

        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
