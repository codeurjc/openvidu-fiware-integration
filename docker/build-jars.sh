#!/bin/bash

# Remove jar files to allow several executions of the script
rm *.jar

cd ../fiware-integration
mvn package -DskipTests

# Copy jar files from fiware-integration Java folder
cp target/fiware*.jar ../docker


cd ../openvidu-filters-client
mvn package -DskipTests

# Copy jar files from openvidu-filters-client Java folder
cp target/openvidu*.jar ../docker

# cd to docker folder
cd ../docker

# Enable extended patterns
shopt -s extglob

# Remove version from .jar file
mv $(echo fiware-!(*-sources|*-javadoc).jar) ./fiware-integration.jar
mv $(echo openvidu-!(*-sources|*-javadoc).jar) ./openvidu-filters-client.jar
