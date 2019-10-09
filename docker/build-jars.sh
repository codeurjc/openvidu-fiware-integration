#!/bin/bash

# Remove jar files to allow several executions of the script
rm *.jar

# Compile fiware-integration
cd ../fiware-integration
mvn clean install -DskipTests

# Compile openvidu-filters-client
cd ../openvidu-filters-client
mvn clean install -DskipTests

# cd to docker folder
cd ../docker


