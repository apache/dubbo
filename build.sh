#! /bin/bash

mvn install -f dubbo-dependency/pom.xml

mvn clean install -Dmaven.test.skip=true