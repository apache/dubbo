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
package org.apache.dubbo.rpc.protocol.grpc;


public class GrpcConstants {

    public static final String DIERCTOR_KEY = "grpc.director";
    public static final String HANDSHAKE_TIMEOUT = "grpc.handshakeTimeout";
    public static final String MAX_INBOUND_MESSAGE_SIZE = "grpc.maxInboundMessageSize";
    public static final String MAX_INBOUND_METADATA_SIZE = "grpc.maxOutboundMessageSize";
    public static final String FLOWCONTROL_WINDOW = "grpc.flowControlWindow";
    public static final String MAX_CONCURRENT_CALLS_PER_CONNECTION = "grpc.maxConcurrentCallsPerConnection";

    public static final String WORKER_THREAD_NUM = "grpc.io.num";
    public static final String BOSS_THREAD_NUM = "grpc.boss.num";
    public static final String CHANNEL_TYPE = "grpc.channel.type";

    public static final String SERVER_INTERCEPTORS = "grpc.serverInterceptors";
    public static final String CLIENT_INTERCEPTORS = "grpc.clientInterceptors";
    public static final String TRANSPORT_FILTERS = "grpc.transportFilters";

    public static final String EXECUTOR = "grpc.executor";

    public static final String CONFIGURATOR = "grpc.configurator";

}
