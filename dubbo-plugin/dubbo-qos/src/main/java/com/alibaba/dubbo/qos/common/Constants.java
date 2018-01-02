/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.qos.common;

public interface Constants {

    int DEFAULT_PORT = 22222;
    // system property for specifying qos port
    String QOS_PORT = "dubbo.qos.port";
    String BR_STR = "\r\n";
    String CLOSE = "close!";

    // system property for whether to accept foreign IP to connect or not
    String ACCEPT_FOREIGN_IP = "dubbo.qos.accept.foreign.ip";
}
