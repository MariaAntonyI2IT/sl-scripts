package com.mdtlabs.migration;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.mdtlabs.migration.script.Diagnosis;
import com.mdtlabs.migration.script.FacilityReportAdmin;
import com.mdtlabs.migration.script.ExcelToDatabase;
import com.mdtlabs.migration.script.HouseholdMemberLink;
import com.mdtlabs.migration.script.HouseholdNumberTypeScript;
import com.mdtlabs.migration.script.HouseholdSequence;
import com.mdtlabs.migration.script.PatientIdUpdate;
import com.mdtlabs.migration.script.SpousePartner;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws SQLException {
        if (args.length == 0) {
            System.out.println("No method specified. Exiting...");
            return;
        }
        // Start timing
        Date startDate = new Date();

        // Read environment properties
        new EnvDetails().readProperties();

        // Execute method based on the first argument
        switch (args[0]) {
            case "updateHouseholdSequence":
                new HouseholdSequence().updateHouseholdSequence();
                break;
            case "updateMemberSequence":
                new HouseholdSequence().updateMemberSequence();
                break;
            case "updatePatientId":
                new PatientIdUpdate().updateMemberSequence();
                break;
            case "householdMemberLink":
                new HouseholdMemberLink().householdMemberLink();
                break;
            case "householdNumberType":
                new HouseholdNumberTypeScript().updateHouseholdNumberType();
                break;
            case "updateSpouseData":
                new SpousePartner().updateSpouseData();
                break;
            case "updateDiagnosisData":
                new Diagnosis().updateDiagnosisData();
            case "facilityReportAdmin":
                new FacilityReportAdmin().updateReportUserOrganization();
            case "updatedMetaInDatabase":
                new ExcelToDatabase().updatedMetaInDatabase();
                break;
            default:
                System.out.println("Unknown method: " + args[0]);
                break;
        }

        // Log time taken
        System.out.println("\nTime taken: " +
                TimeUnit.MILLISECONDS.toSeconds((new Date().getTime() - startDate.getTime())) + " seconds");
    }
}
