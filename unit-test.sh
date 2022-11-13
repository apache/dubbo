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

echo "Found ${#submodules[@]} poms from files"
echo "Skip ${#skip_modules[@]} modules"

if [ $# -eq 2 ]; then
    current_executor=$1
    total_executor=$2
    total_modules=${#submodules[@]}

    for (( i = 0; i < total_modules; i++ )); do
#    for (( i = 0; i < 4; i++ )); do
      case_count=$((i % total_executor))
        if [ $case_count -eq "$current_executor" ]; then
          docker run --rm -v $(pwd):/space -v $HOME/.m2:/root/.m2 -w /space openjdk:17 bash .tmp/script-$i.sh
          exit_code=$?
          if [ $exit_code -ne 0 ]; then
            echo "Failed to execute ${submodules[$i]} test cases"
            echo $exit_code > .tmp/exit_code-$current_executor
            exit $exit_code
          fi
        fi
    done
    echo 0 > .tmp/exit_code-$current_executor
    exit 0
fi

./mvnw --batch-mode -no-transfer-progress clean install -T 2C -Dmaven.test.skip=true
mkdir .tmp

for (( i = 0; i < ${#submodules[@]}; i++ )); do
#for (( i = 0; i < 8; i++ )); do
  if [ ${submodules[$i]} != "" ]; then
    case_num=$((i + 1))
    echo "Generate ${submodules[$i]} test cases script $case_num / ${#submodules[@]}"
    echo "cd /space/${submodules[$i]} && /space/mvnw --offline --batch-mode -no-transfer-progress clean test verify -Pjacoco" > .tmp/script-$i.sh
  fi
done

forks=4
sub_pids=[]
for (( i = 0; i < $forks; i++ )); do
    bash ./unit-test.sh $i $forks &
    sub_pids[$i]=$!
done

for (( i = 0; i < $forks; i++ )); do
    wait ${sub_pids[$i]}
    exit_code=$(cat .tmp/exit_code-$i)
    if [ $exit_code -ne '0' ]; then
      echo "Failed to execute $i sub jobs"
      exit $exit_code
    fi
done


echo "All Test passed."
