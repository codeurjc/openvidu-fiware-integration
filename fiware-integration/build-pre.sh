#!/bin/bash
echo "PRE_BUILD - INIT"

rm -rf ./src/main/resources/static/
mkdir -p ./src/main/resources/static/

cp -r ./src/frontend/* ./src/main/resources/static/
echo "PRE_BUILD - resources copied to resources/static"

ls ./src/main/resources/

{
	sed -i '' -e 's/spring\.resources\.static-locations/# spring\.resources\.static-locations/g' ./src/main/resources/application.properties
} || {
	echo "Warning: error on sed"
}
echo "PRE_BUILD - application.properties modified"

# Set the minified versions to production.
find ./src/main/resources/static -name "*.min.js" | while read oldFileName; do
  newFileName=$(echo $oldFileName | sed s/.min//g)
  mv $oldFileName $newFileName
done
echo "POST_BUILD - js adapted to use minified versions"

echo "PRE_BUILD - END"
