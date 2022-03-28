#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

# Create graalvm environment for running dubbo native projects

FROM maven:3-jdk-11-slim

WORKDIR /opt

RUN apt-get update \
    && apt-get install -y gcc zlib1g-dev libstdc++-10-dev \
    && curl -L -o /opt/graalvm-ce.tar.gz "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.1.0/graalvm-ce-java11-linux-amd64-21.1.0.tar.gz" \
    && tar -xf graalvm-ce.tar.gz \
    && /opt/graalvm-ce-java11-21.1.0/bin/gu install native-image \
    && rm graalvm-ce.tar.gz

ENV PATH=/opt/graalvm-ce-java11-21.1.0/bin/:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
ENV JAVA_HOME=/opt/graalvm-ce-java11-21.1.0
