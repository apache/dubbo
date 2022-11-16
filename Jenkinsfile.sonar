/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
pipeline {
    agent {
        label 'ubuntu'
    }

    tools {
        maven 'maven_3_latest'
        jdk 'jdk_11_latest'
    }

    stages {
        stage('Code Quality') {
            steps {
                echo 'Checking Code Quality on SonarCloud'
                script {
                    // Main parameters
                    def sonarcloudParams=""
                    if ( env.BRANCH_NAME.startsWith("PR-") ) {
                        // this is a pull request
                        withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                            sh './mvnw --batch-mode --no-snapshot-updates -e --no-transfer-progress --fail-fast clean verify sonar:sonar -Dmaven.test.skip=true -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization=apache -Dsonar.projectKey=apache_dubbo -Dsonar.coverage.jacoco.xmlReportPaths=dubbo-test/dubbo-dependencies-all/target/site/jacoco-aggregate/jacoco.xml -Dsonar.pullrequest.branch=${CHANGE_BRANCH} -Dsonar.pullrequest.base=${CHANGE_TARGET} -Dsonar.pullrequest.key=${CHANGE_ID} -Dsonar.login=${SONAR_TOKEN}'
                        }
                    } else {
                        // this is just a branch
                        withCredentials([string(credentialsId: 'sonarcloud-token', variable: 'SONAR_TOKEN')]) {
                            sh './mvnw --batch-mode --no-snapshot-updates -e --no-transfer-progress --fail-fast clean verify sonar:sonar -Dmaven.test.skip=true -Dsonar.host.url=https://sonarcloud.io -Dsonar.organization=apache -Dsonar.projectKey=apache_dubbo -Dsonar.coverage.jacoco.xmlReportPaths=dubbo-test/dubbo-dependencies-all/target/site/jacoco-aggregate/jacoco.xml -Dsonar.branch.name=${BRANCH_NAME} -Dsonar.login=${SONAR_TOKEN}'
                        }
                    }
                }
            }
        }
    }
}
