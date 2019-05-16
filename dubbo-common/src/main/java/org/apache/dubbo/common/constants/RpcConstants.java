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
public interface RpcConstants {
    // BEGIN dubbo-rpc-hessian
    String HESSIAN2_REQUEST_KEY = "hessian2.request";

    boolean DEFAULT_HESSIAN2_REQUEST = false;

    String HESSIAN_OVERLOAD_METHOD_KEY = "hessian.overload.method";

    boolean DEFAULT_HESSIAN_OVERLOAD_METHOD = false;

    String DEFAULT_HTTP_CLIENT = "jdk";

    String DEFAULT_HTTP_SERVER = "servlet";

    String DEFAULT_HTTP_SERIALIZATION = "json";
    // END dubbo-rpc-hessian

    // BEGIN dubbo-rpc-dubbo
    String SHARE_CONNECTIONS_KEY = "shareconnections";

    /**
     * By default, a consumer JVM instance and a provider JVM instance share a long TCP connection (except when connections are set),
     * which can set the number of long TCP connections shared to avoid the bottleneck of sharing a single long TCP connection.
     */
    String DEFAULT_SHARE_CONNECTIONS = "1";

    String INPUT_KEY = "input";

    String OUTPUT_KEY = "output";

    String DECODE_IN_IO_THREAD_KEY = "decode.in.io";

    boolean DEFAULT_DECODE_IN_IO_THREAD = true;

    /**
     * callback inst id
     */
    String CALLBACK_SERVICE_KEY = "callback.service.instid";

    /**
     * The limit of callback service instances for one interface on every client
     */
    String CALLBACK_INSTANCES_LIMIT_KEY = "callbacks";

    /**
     * The default limit number for callback service instances
     *
     * @see #CALLBACK_INSTANCES_LIMIT_KEY
     */
    int DEFAULT_CALLBACK_INSTANCES = 1;

    String CALLBACK_SERVICE_PROXY_KEY = "callback.service.proxy";

    String IS_CALLBACK_SERVICE = "is_callback_service";

    /**
     * Invokers in channel's callback
     */
    String CHANNEL_CALLBACK_KEY = "channel.callback.invokers.key";

    /**
     * The initial state for lazy connection
     */
    String LAZY_CONNECT_INITIAL_STATE_KEY = "connect.lazy.initial.state";

    /**
     * The default value of lazy connection's initial state: true
     *
     * @see #LAZY_CONNECT_INITIAL_STATE_KEY
     */
    boolean DEFAULT_LAZY_CONNECT_INITIAL_STATE = true;

    String OPTIMIZER_KEY = "optimizer";
    // END dubbo-rpc-dubbo


    // BEGIN dubbo-rpc-api
    String DUBBO_VERSION_KEY = "dubbo";

    String LOCAL_KEY = "local";

    String STUB_KEY = "stub";

    String MOCK_KEY = "mock";

    String DEPRECATED_KEY = "deprecated";

    String $INVOKE = "$invoke";

    String $ECHO = "$echo";

    String RETURN_PREFIX = "return ";

    String THROW_PREFIX = "throw";

    String FAIL_PREFIX = "fail:";

    String FORCE_PREFIX = "force:";

    String MERGER_KEY = "merger";

    String IS_SERVER_KEY = "isserver";

    String FORCE_USE_TAG = "dubbo.force.tag";

    String GENERIC_SERIALIZATION_NATIVE_JAVA = "nativejava";

    String GENERIC_SERIALIZATION_DEFAULT = "true";

    String GENERIC_SERIALIZATION_BEAN = "bean";

    String GENERIC_SERIALIZATION_PROTOBUF = "protobuf-json";

    String TPS_LIMIT_RATE_KEY = "tps";

    String TPS_LIMIT_INTERVAL_KEY = "tps.interval";

    long DEFAULT_TPS_LIMIT_INTERVAL = 60 * 1000;

    String AUTO_ATTACH_INVOCATIONID_KEY = "invocationid.autoattach";

    String STUB_EVENT_KEY = "dubbo.stub.event";

    boolean DEFAULT_STUB_EVENT = false;

    String STUB_EVENT_METHODS_KEY = "dubbo.stub.event.methods";

    String PROXY_KEY = "proxy";

    String EXECUTES_KEY = "executes";

    String REFERENCE_FILTER_KEY = "reference.filter";

    String INVOKER_LISTENER_KEY = "invoker.listener";

    String SERVICE_FILTER_KEY = "service.filter";

    String EXPORTER_LISTENER_KEY = "exporter.listener";

    String ACCESS_LOG_KEY = "accesslog";

    String ACTIVES_KEY = "actives";

    String CONNECTIONS_KEY = "connections";

    String ID_KEY = "id";

    String ASYNC_KEY = "async";

    String FUTURE_GENERATED_KEY = "future_generated";

    String FUTURE_RETURNTYPE_KEY = "future_returntype";

    String RETURN_KEY = "return";

    String TOKEN_KEY = "token";

    String INTERFACES = "interfaces";

    String GENERIC_KEY = "generic";

    String LOCAL_PROTOCOL = "injvm";
    // END dubbo-rpc-api


    // BEGIN dubbo-rpc-rest
    String KEEP_ALIVE_KEY = "keepalive";

    boolean DEFAULT_KEEP_ALIVE = true;

    String EXTENSION_KEY = "extension";
    // END dubbo-rpc-rest
}
