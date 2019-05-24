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

    String INPUT_KEY = "input";

    String OUTPUT_KEY = "output";

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

    String DUBBO_VERSION_KEY = "dubbo";

    String $INVOKE = "$invoke";

    String $INVOKE_ASYNC = "$invokeAsync";

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

}
