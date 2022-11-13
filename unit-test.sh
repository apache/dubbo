#!/bin/sh
set -m
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
available_submodules=()
available_count=0

for (( i = 0; i < ${#submodules[@]}; i++ )); do
  target="${submodules[$i]}/src/test"
  if [ -d $target ]; then
    available_submodules[$available_count]=${submodules[$i]}
    available_count=$((available_count+1))
  fi
done

echo "Found ${#available_submodules[@]} poms from files"
echo "Skip ${#skip_modules[@]} modules"

if [ $# -eq 2 ]; then
    current_executor=$1
    total_executor=$2
    total_modules=${#available_submodules[@]}

    docker rm "dubbo-test-$current_executor" -f
    for (( i = 0; i < total_modules; i++ )); do
#    for (( i = 0; i < 4; i++ )); do
      case_count=$((i % total_executor))
        if [ $case_count -eq "$current_executor" ]; then
          case_num=$((i + 1))
          echo "Executing ${available_submodules[$i]} test cases $case_num / ${#available_submodules[@]}"
          container_id=$(docker run -d --name "dubbo-test-$current_executor" -v $(pwd):/space -v $HOME/.m2:/root/.m2 -w /space openjdk:11 bash .tmp/script-$i.sh)
          exit_code=$(docker wait "$container_id")
          if [ $exit_code -ne 0 ]; then
            echo "Failed to execute ${available_submodules[$i]} test cases $case_num / ${#available_submodules[@]}"
            echo $exit_code > .tmp/exit_code-$current_executor

            docker logs "$container_id"
            docker logs "$container_id" > .tmp/logs-$case_num.txt 2>&1
            docker rm "$container_id"

            exit $exit_code
          fi
          docker logs "$container_id" > .tmp/logs-$case_num.txt 2>&1
          docker rm "$container_id"
          echo "Success run ${available_submodules[$i]} test cases $case_num / ${#available_submodules[@]}"
        fi
    done
    echo 0 > .tmp/exit_code-$current_executor
    exit 0
fi

./mvnw --batch-mode -no-transfer-progress clean install -T 2C -Dmaven.test.skip=true
mkdir .tmp

for (( i = 0; i < ${#available_submodules[@]}; i++ )); do
#for (( i = 0; i < 8; i++ )); do
  if [ ${available_submodules[$i]} != "" ]; then
    case_num=$((i + 1))
    echo "Generate ${available_submodules[$i]} test cases script $case_num / ${#available_submodules[@]}"
    echo "cd /space/${available_submodules[$i]} && /space/mvnw --offline --batch-mode -no-transfer-progress clean test verify -Pjacoco" > .tmp/script-$i.sh
  fi
done

forks=8
sub_pids=[]
for (( i = 0; i < $forks; i++ )); do
    bash ./unit-test.sh $i $forks &
    sub_pids[$i]=$!
done

_kill() {
    echo "Receive sigterm"
    for (( i = 0; i < $forks; i++ )); do
        kill -9 ${sub_pids[$i]}
        docker stop -t 0 "dubbo-test-$i"
        docker rm -f "dubbo-test-$i"
    done
    exit 143
}

trap _kill SIGTERM
trap _kill SIGINT
trap _kill SIGQUIT

parent_exit_code=0
for (( i = 0; i < $forks; i++ )); do
    count=2
    while [ $count -ne 0 ]; do
        count=`ps -ef |grep ${sub_pids[$i]} |grep -v "grep" |wc -l`
        sleep 1
    done
    wait ${sub_pids[$i]}
    exit_code=$(cat .tmp/exit_code-$i)
    if [ $exit_code -ne '0' ]; then
      echo "Failed to execute $i sub jobs"
      parent_exit_code=$exit_code
    fi
done

_kill
exit $parent_exit_code

echo "All Test passed."
