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
package org.apache.dubbo.config.spring.reference;

/**
 * Attribute names of {@link org.apache.dubbo.config.annotation.DubboReference}
 * and {@link org.apache.dubbo.config.ReferenceConfig}
 */
public interface ReferenceAttributes {

    String ID = "id";

    String INTERFACE = "interface";

    String INTERFACE_NAME = "interfaceName";

    String INTERFACE_CLASS = "interfaceClass";

    String ACTUAL_INTERFACE = "actualInterface";

    String GENERIC = "generic";

    String REGISTRY = "registry";

    String REGISTRIES = "registries";

    String REGISTRY_IDS = "registryIds";

    String GROUP = "group";

    String VERSION = "version";

    String ARGUMENTS = "arguments";

    String METHODS = "methods";

    String PARAMETERS = "parameters";

    String PROVIDED_BY = "providedBy";

    String PROVIDER_PORT = "providerPort";

    String URL = "url";

    String CLIENT = "client";

//    /**
//     * When enable, prefer to call local service in the same JVM if it's present, default value is true
//     * @deprecated using scope="local" or scope="remote" instead
//     */
//    @Deprecated
    String INJVM = "injvm";

    String CHECK = "check";

    String INIT = "init";

    String LAZY = "lazy";

    String STUBEVENT = "stubevent";

    String RECONNECT = "reconnect";

    String STICKY = "sticky";

    String PROXY = "proxy";

    String STUB = "stub";

    String CLUSTER = "cluster";

    String CONNECTIONS = "connections";

    String CALLBACKS = "callbacks";

    String ONCONNECT = "onconnect";

    String ONDISCONNECT = "ondisconnect";

    String OWNER = "owner";

    String LAYER = "layer";

    String RETRIES = "retries";

    String LOAD_BALANCE = "loadbalance";

    String ASYNC = "async";

    String ACTIVES = "actives";

    String SENT = "sent";

    String MOCK = "mock";

    String VALIDATION = "validation";

    String TIMEOUT = "timeout";

    String CACHE = "cache";

    String FILTER = "filter";

    String LISTENER = "listener";

    String APPLICATION = "application";

    String MODULE = "module";

    String CONSUMER = "consumer";

    String MONITOR = "monitor";

    String PROTOCOL = "protocol";

    String TAG = "tag";

    String MERGER = "merger";

    String SERVICES = "services";

    String SCOPE = "scope";
}
