#!/bin/bash

APPEND_ARG=""
FOLDER="./"
LINE_FLAG="=============================================="

if [ -n "$1" ]; then
    echo "checking module $1"
    APPEND_ARG="-f $1"
    FOLDER="$1"
else
    echo "checking whole project"
fi

echo "Running command: ./mvnw clean package -DskipTests=true -PlicenseCheck $APPEND_ARG"

./mvnw clean package -DskipTests=true -PlicenseCheck $APPEND_ARG

status=$?
if [ $status -eq 0 ]; then
  echo "mvn command exec success"
else
  echo "mvn command exec fail"
  exit 1
fi

rm -rf ./license-list.txt
LICENSE_FILES=`find $FOLDER -type f -name "THIRD-PARTY.txt"|grep generated-sources`

echo "Find license files: \n $LICENSE_FILES"

for i in $LICENSE_FILES
    do
        echo "$LINE_FLAG" >> ./license-list.txt
        echo $i >> ./license-list.txt
        cat $i >> ./license-list.txt
    done

echo "license files generated at ./license-list.txt"

cat ./license-list.txt
