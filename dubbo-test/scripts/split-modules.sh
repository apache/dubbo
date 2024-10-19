#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

readarray -t modules < <(find . -name "pom.xml" | while read -r pom; do
    module_dir=$(dirname "$pom")
    if [ -d "$module_dir/src/test" ] && [[ "$module_dir" != *"plugin-loom" ]] && [[ "$module_dir" != *"config-spring6" ]]; then
        echo "${module_dir#./}"
    fi
done)

readarray -t modules < <(printf '%s\n' "${modules[@]}" | shuf --random-source=<(yes 1))

jobs=$1
jobs_dir=$2/test/jobs

total=${#modules[@]}
size=$((total / jobs))
remainder=$((total % jobs))

mkdir -p "$jobs_dir"

IFS=','
for ((i=0; i<jobs; i++)); do
    start=$((i * size + (i < remainder ? i : remainder)))
    end=$((start + size + (i < remainder ? 1 : 0)))
    echo "${modules[*]:start:end-start}" > "$jobs_dir/test_modules_$((i+1)).txt"
done
