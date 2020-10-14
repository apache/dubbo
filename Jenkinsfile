import groovy.json.JsonSlurper

pipeline {
    agent {
        node {
            label 'ubuntu'
        }
    }

    options {
        buildDiscarder(logRotator(daysToKeepStr: '14', artifactNumToKeepStr: '10'))
    }

    environment {
        JAVA_HOME = "${tool 'JDK 1.8 (latest)'}"
    }

    tools {
        maven 'Maven 3 (latest)'
        jdk 'JDK 1.8 (latest)'
    }

    triggers {
        cron '''TZ=Asia/Shanghai
        H 2,14 * * *'''
        pollSCM '''TZ=Asia/Shanghai
        H H/2 * * *'''
    }


    stages {
        stage('Clone') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CloneOption', noTags: true, reference: '', shallow: true]], gitTool: 'Default', submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/apache/dubbo.git']]])
            }
        }

        stage('Duplicate deploy check') {
            steps {
                script {
                    def deployedCommitId = sh(returnStdout: true, script: "curl --silent https://builds.apache.org/job/Apache%20Dubbo/job/${env.JOB_BASE_NAME}/lastSuccessfulBuild/artifact/DEPLOY_COMMIT_ID || true").trim()
                    env.DEPLOYED_COMMIT_ID = deployedCommitId
                    def commitId = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                    env.COMMIT_ID = commitId

                    if (commitId == deployedCommitId) {
                        env.STATUS_CHECK = "false"
                        println "Latest deployed commit id is $deployedCommitId, Skip deployment this time"
                    } else {
                        env.STATUS_CHECK = "true"
                        println "Current commit id hasn't been deployed, continue"
                    }
                }
            }
        }

        stage('Commit status check') {
            when {
                expression {
                    return env.STATUS_CHECK == "true";
                }
            }
            steps {
                script {
                    def commitId = env.COMMIT_ID
                    println "Current commit id: $commitId"

                    def commitStatusJson = sh(script: "curl --silent https://api.github.com/repos/apache/dubbo/commits/$commitId/status", returnStdout: true).trim()
                    println "Commit status: \r\n$commitStatusJson"

                    def jsonSlurper = new JsonSlurper()
                    def jsonObject = jsonSlurper.parseText(commitStatusJson)

                    def status = jsonObject.state

                    println "Current commit status is $status"

                    if (status == "success") {
                        env.STATUS_CHECK = "true"
                        println "Continue to deploy snapshot"
                    } else {
                        env.STATUS_CHECK = "false"
                        println "Current commit status not allow to deploy snapshot"
                    }
                }
            }
        }

        stage('Snapshot version check') {
            when {
                expression {
                    return env.STATUS_CHECK == "true";
                }
            }
            steps {
                sh 'env'
                sh 'java -version'
                sh './mvnw clean install -pl "dubbo-dependencies-bom" && ./mvnw clean install -DskipTests=true && ./mvnw clean validate -Psnapshot-ci-deploy -pl "dubbo-all"'
            }
        }

        stage('Deploy snapshot') {
            when {
                expression {
                    return env.STATUS_CHECK == "true";
                }
            }
            steps {
                timeout(40) {
                    sh './mvnw --version'
                    sh './mvnw clean package deploy -pl dubbo-dependencies-bom && ./mvnw clean source:jar javadoc:jar package deploy -DskipTests=true'
                }
            }
        }

        stage('Save deployed commit id') {
            steps {
                script {
                    if (env.STATUS_CHECK != "true") {
                        println "Not pass status check"
                        env.COMMIT_ID = env.DEPLOYED_COMMIT_ID
                    }
                }
                writeFile file: 'DEPLOY_COMMIT_ID', text: "${env.COMMIT_ID}"
                archiveArtifacts 'DEPLOY_COMMIT_ID'
            }
        }
    }

    post {
        failure {
            mail bcc: '', body: '''Project: ${env.JOB_NAME}
            Build Number: ${env.BUILD_NUMBER}
            URL: ${env.BUILD_URL}''', cc: '', from: '', replyTo: '', subject: 'Apache Dubbo snapshot deployment fail', to: 'dev@dubbo.apache.org'
        }
    }

}
