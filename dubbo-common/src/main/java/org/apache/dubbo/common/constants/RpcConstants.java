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

package org.apache.dubbo.common.constants;

/**
 * RpcConstants
 */
public class RpcConstants {
    // BEGIN dubbo-rpc-hessian
    public static final String HESSIAN2_REQUEST_KEY = "hessian2.request";

    public static final boolean DEFAULT_HESSIAN2_REQUEST = false;

    public static final String HESSIAN_OVERLOAD_METHOD_KEY = "hessian.overload.method";

    public static final boolean DEFAULT_HESSIAN_OVERLOAD_METHOD = false;

    public static final String DEFAULT_HTTP_CLIENT = "jdk";

    public static final String DEFAULT_HTTP_SERVER = "servlet";

    public static final String DEFAULT_HTTP_SERIALIZATION = "json";
    // END dubbo-rpc-hessian

    // BEGIN dubbo-rpc-dubbo
    public static final String SHARE_CONNECTIONS_KEY = "shareconnections";

    /**
     * By default, a consumer JVM instance and a provider JVM instance share a long TCP connection (except when connections are set),
     * which can set the number of long TCP connections shared to avoid the bottleneck of sharing a single long TCP connection.
     */
    public static final String DEFAULT_SHARE_CONNECTIONS = "1";

    public static final String INPUT_KEY = "input";

    public static final String OUTPUT_KEY = "output";

    public static final String DECODE_IN_IO_THREAD_KEY = "decode.in.io";

    public static final boolean DEFAULT_DECODE_IN_IO_THREAD = true;

    /**
     * callback inst id
     */
    public static final String CALLBACK_SERVICE_KEY = "callback.service.instid";

    /**
     * The limit of callback service instances for one interface on every client
     */
    public static final String CALLBACK_INSTANCES_LIMIT_KEY = "callbacks";

    /**
     * The default limit number for callback service instances
     *
     * @see #CALLBACK_INSTANCES_LIMIT_KEY
     */
    public static final int DEFAULT_CALLBACK_INSTANCES = 1;

    public static final String CALLBACK_SERVICE_PROXY_KEY = "callback.service.proxy";

    public static final String IS_CALLBACK_SERVICE = "is_callback_service";

    /**
     * Invokers in channel's callback
     */
    public static final String CHANNEL_CALLBACK_KEY = "channel.callback.invokers.key";

    /**
     * The initial state for lazy connection
     */
    public static final String LAZY_CONNECT_INITIAL_STATE_KEY = "connect.lazy.initial.state";

    /**
     * The default value of lazy connection's initial state: true
     *
     * @see #LAZY_CONNECT_INITIAL_STATE_KEY
     */
    public static final boolean DEFAULT_LAZY_CONNECT_INITIAL_STATE = true;

    public static final String OPTIMIZER_KEY = "optimizer";
    // END dubbo-rpc-dubbo


    // BEGIN dubbo-rpc-api
    public static final String DUBBO_VERSION_KEY = "dubbo";

    public static final String LOCAL_KEY = "local";

    public static final String STUB_KEY = "stub";

    public static final String MOCK_KEY = "mock";

    public static final String DEPRECATED_KEY = "deprecated";

    public static final String $INVOKE = "$invoke";

    public static final String $ECHO = "$echo";

    public static final String RETURN_PREFIX = "return ";

    public static final String THROW_PREFIX = "throw";

    public static final String FAIL_PREFIX = "fail:";

    public static final String FORCE_PREFIX = "force:";

    public static final String MERGER_KEY = "merger";

    public static final String IS_SERVER_KEY = "isserver";

    public static final String FORCE_USE_TAG = "dubbo.force.tag";

    public static final String GENERIC_SERIALIZATION_NATIVE_JAVA = "nativejava";

    public static final String GENERIC_SERIALIZATION_DEFAULT = "true";

    public static final String GENERIC_SERIALIZATION_BEAN = "bean";

    public static final String GENERIC_SERIALIZATION_PROTOBUF = "protobuf-json";

    public static final String TPS_LIMIT_RATE_KEY = "tps";

    public static final String TPS_LIMIT_INTERVAL_KEY = "tps.interval";

    public static final long DEFAULT_TPS_LIMIT_INTERVAL = 60 * 1000;

    public static final String AUTO_ATTACH_INVOCATIONID_KEY = "invocationid.autoattach";

    public static final String STUB_EVENT_KEY = "dubbo.stub.event";

    public static final boolean DEFAULT_STUB_EVENT = false;

    public static final String STUB_EVENT_METHODS_KEY = "dubbo.stub.event.methods";

    public static final String PROXY_KEY = "proxy";

    public static final String EXECUTES_KEY = "executes";

    public static final String REFERENCE_FILTER_KEY = "reference.filter";

    public static final String INVOKER_LISTENER_KEY = "invoker.listener";

    public static final String SERVICE_FILTER_KEY = "service.filter";

    public static final String EXPORTER_LISTENER_KEY = "exporter.listener";

    public static final String ACCESS_LOG_KEY = "accesslog";

    public static final String ACTIVES_KEY = "actives";

    public static final String CONNECTIONS_KEY = "connections";

    public static final String ID_KEY = "id";

    public static final String ASYNC_KEY = "async";

    public static final String FUTURE_GENERATED_KEY = "future_generated";

    public static final String FUTURE_RETURNTYPE_KEY = "future_returntype";

    public static final String RETURN_KEY = "return";

    public static final String TOKEN_KEY = "token";

    public static final String INTERFACES = "interfaces";

    public static final String GENERIC_KEY = "generic";

    public static final String LOCAL_PROTOCOL = "injvm";
    // END dubbo-rpc-api


    // BEGIN dubbo-rpc-rest
    public static final String KEEP_ALIVE_KEY = "keepalive";

    public static final boolean DEFAULT_KEEP_ALIVE = true;

    public static final String EXTENSION_KEY = "extension";
    // END dubbo-rpc-rest
}
