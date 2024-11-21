Step 1: Build the Project and Copy Dependencies

First, run the following Maven command to clean, build, and copy dependencies to the target directory:

    mvn clean install dependency:copy-dependencies

Step 2: Run the Data Migration Application

Run the migration application with the following command and also pass your method name as argument,
which allocates 12 GB of memory (adjust Xmx as needed for your environment):

    java -Xmx12288m -cp target/dependency/*:target/data-migration-1.0-SNAPSHOT.jar com.mdtlabs.migration.App methodName

Step 3: Never forget to add your script method name in App.java class

Method Names

- updateHouseholdSequence
- updateMemberSequence
- updateSpouseData
- updateDiagnosisData
