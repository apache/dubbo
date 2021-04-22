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
package org.apache.dubbo.rpc.protocol.tri;

public interface TripleConstant {
    String AUTHORITY_KEY = ":authority";
    String PATH_KEY = ":path";
    String HTTP_STATUS_KEY = "http-status";
    String STATUS_KEY = "grpc-status";
    String MESSAGE_KEY = "grpc-message";
    String TIMEOUT = "grpc-timeout";
    String CONTENT_TYPE_KEY = "content-type";
    String CONTENT_PROTO = "application/grpc+proto";
    String APPLICATION_GRPC = "application/grpc";
    String TRICE_ID_KEY = "tri-trace-traceid";
    String RPC_ID_KEY = "tri-trace-rpcid";
    String CONSUMER_APP_NAME_KEY = "tri-consumer-appname";
    String UNIT_INFO_KEY = "tri-unit-info";
    String SERVICE_VERSION = "tri-service-version";
    String SERVICE_GROUP = "tri-service-group";
    String TRI_VERSION = "1.0.0";

}
