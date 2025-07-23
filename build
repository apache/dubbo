#!/bin/sh

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

set -eu

cd "$(dirname "$0")"

export MAVEN_OPTS="\
-Xms2g \
-Xmx2g \
-XX:+UseG1GC \
-XX:InitiatingHeapOccupancyPercent=45 \
-XX:+UseStringDeduplication \
-XX:-TieredCompilation \
-XX:TieredStopAtLevel=1 \
-Dmaven.build.cache.enabled=true \
-Dmaven.build.cache.lazyRestore=true \
-Dmaven.compiler.useIncrementalCompilation=false \
-Dmaven.test.skip=true \
-Dcheckstyle.skip=true \
-Dcheckstyle_unix.skip=true \
-Drat.skip=true \
-Dmaven.javadoc.skip=true
"

CMD="./mvnw -e --batch-mode --no-snapshot-updates --fail-fast -T 2C"
ARGS=""
MODULES=""
PROFILES="sources,skip-spotless"
DEFAULT_MODULES="dubbo-distribution/dubbo-all,dubbo-spring-boot/dubbo-spring-boot-starter"

print_help() {
    echo "Usage: $0 [options]"
    echo "Fast local compilation with incremental build and caching"
    echo "Options:"
    echo "  -c    Execute clean goal (removes build artifacts)"
    echo "  -p    Execute compile goal (compiles the source code)"
    echo "  -i    Execute install goal (builds and installs the project)"
    echo "  -t    Execute test goal (runs the tests)"
    echo "  -s    Execute spotless:apply (format the code)"
    echo "  -d    Execute dependency:tree (displays the dependency tree)"
    echo "  -m    Specify modules, default is $DEFAULT_MODULES"
    echo "  -f    Specify profiles, default is $PROFILES"
    echo "  -h    Display this help message"
    echo ""
    echo "Examples:"
    echo "  $0                        Execute install goal compilation"
    echo "  $0 -m                     Execute a minimal compilation"
    echo "  $0 -ci                    Execute clean, install goals compilation"
    echo "  $0 -s                     Execute spotless:apply"
    echo "  $0 -d                     Display the dependency tree"
    echo "  $0 -t -m dubbo-config     Execute test goal for dubbo-config module"
    echo "  $0 -cp -m dubbo-common    Execute clean, compile the dubbo-common module"
    exit 0
}

while getopts ":cpitstdm:f:h" opt; do
  case $opt in
    c)
      ARGS="$ARGS clean"
      ;;
    p)
      ARGS="$ARGS compile"
      ;;
    i)
      ARGS="$ARGS install"
      ;;
    t)
      ARGS="$ARGS test"
      export MAVEN_OPTS=$(echo "$MAVEN_OPTS" | sed 's/-Dmaven\.test\.skip=true/-Dmaven.test.skip=false/')
      ;;
    s)
      ARGS="$ARGS spotless:apply"
      PROFILES="sources"
      ;;
    d)
      ARGS="$ARGS dependency:tree"
      ;;
    m)
      MODULES=" -pl $OPTARG -am"
      ;;
    f)
      PROFILES="$OPTARG"
      ;;
    h)
      print_help
      ;;
    *)
      if [ "$OPTARG" = "m" ]; then
        MODULES=" -pl $DEFAULT_MODULES -am"
      else
        ARGS="$ARGS $@"
      fi
      ;;
  esac
done

if [ -z "$ARGS" ] ; then
  ARGS=" install"
fi

set -x
$CMD$ARGS$MODULES -P $PROFILES
