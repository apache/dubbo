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

FROM openjdk:8-jdk
ADD ./target/dubbo-demo-xds-provider-3.3.0-beta.2-SNAPSHOT.jar dubbo-demo-xds-provider-3.3.0-beta.2-SNAPSHOT.jar
CMD java -jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=31001 /dubbo-demo-xds-provider-3.3.0-beta.2-SNAPSHOT.jar
