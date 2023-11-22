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
package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

public enum GrpcHeaderNames {
    GRPC_STATUS("grpc-status"),
    GRPC_MESSAGE("grpc-message"),
    GRPC_ENCODING("grpc-encoding"), //client request compress type
    GRPC_ACCEPT_ENCODING("grpc-accept-encoding"), // client required response compress type
    GRPC_TIMEOUT("grpc-timeout"),
    ;

    private final String name;

    GrpcHeaderNames(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
