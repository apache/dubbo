#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

#mvn --batch-mode -no-transfer-progress dependency:go-offline

data_1=$(find . -name pom.xml | cut -c 3- | rev | cut -c 9- | rev | sort)

submodules=($data_1)
skip_modules[0]="dubbo-all"
skip_modules[1]="dubbo-apache-release"
skip_modules[2]="dubbo-core-spi"

echo "Found ${#submodules[@]} poms from files"
echo "Skip ${#skip_modules[@]} modules"

case_count=0
case_range=${CASE_RANGE:-6}
current_role=${CURRENT_ROLE:-0}

for (( i = 0; i < ${#submodules[@]}; i++ )); do
  if [ ${submodules[$i]} != "" ]; then
    if [ $case_count -eq $current_role ]; then
      echo "Execute ${submodules[$i]} test cases $i / ${#submodules[@]}"
      last_name=$(echo ${submodules[$i]} | awk -F'/' '{print $NF}')
      should_skip=false
      for (( j = 0; j < ${#skip_modules[@]}; j++ )); do
        if [ "${skip_modules[$j]}" = "$last_name" ]; then
          should_skip=true
          break
        fi
      done
      if [ $should_skip = true ]; then
          echo "Skip ${submodules[$i]} due to it is not a valid module"
          continue
      fi
      ./mvnw -pl ${submodules[$i]} --batch-mode --no-snapshot-updates -e --no-transfer-progress --fail-fast clean test verify -Pjacoco -DskipTests=false -DskipIntegrationTests=false -Dcheckstyle.skip=false -Dcheckstyle_unix.skip=false -Drat.skip=false -Dmaven.javadoc.skip=true -DembeddedZookeeperPath=$(pwd)/.tmp/zookeeper
      exit_code=$?
      if [ $exit_code -ne 0 ]; then
        echo "Failed to execute ${submodules[$i]} test cases"
          exit $exit_code
      fi
    fi
    case_count=$((case_count + 1))
    case_count=$((case_count % case_range))
  fi
done

echo "All Test passed."
