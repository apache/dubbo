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

package org.apache.dubbo.config;

import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP_WHITELIST_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST_COMPATIBLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT_COMPATIBLE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;

/**
 *
 */
public interface Constants {

    String STATUS_KEY = "status";

    String CONTEXTPATH_KEY = "contextpath";

    String LISTENER_KEY = "listener";

    String LAYER_KEY = "layer";

    // General

    /**
     * Config id
     */
    String ID = "id";

    /**
     * Application name;
     */
    String NAME = "name";

    /**
     * Application owner name;
     */
    String OWNER = "owner";

    /**
     * Running application organization name.
     */
    String ORGANIZATION = "organization";

    /**
     * Application architecture name.
     */
    String ARCHITECTURE = "architecture";

    /**
     * Environment name
     */
    String ENVIRONMENT = "environment";

    /**
     * Test environment key.
     */
    String TEST_ENVIRONMENT = "test";

    /**
     * Development environment key.
     */
    String DEVELOPMENT_ENVIRONMENT = "develop";

    /**
     * Production environment key.
     */
    String PRODUCTION_ENVIRONMENT = "product";

    String CONFIG_CONFIGFILE_KEY = "config-file";
    String CONFIG_ENABLE_KEY = "highest-priority";
    String CONFIG_APP_CONFIGFILE_KEY = "app-config-file";

    String MULTICAST = "multicast";


    String DUBBO_IP_TO_REGISTRY = "DUBBO_IP_TO_REGISTRY";

    String DUBBO_PORT_TO_REGISTRY = "DUBBO_PORT_TO_REGISTRY";


    String DUBBO_PORT_TO_BIND = "DUBBO_PORT_TO_BIND";

    String SCOPE_NONE = "none";

    String ON_INVOKE_METHOD_PARAMETER_KEY = "oninvoke.method";

    String ON_RETURN_METHOD_PARAMETER_KEY = "onreturn.method";

    String ON_THROW_METHOD_PARAMETER_KEY = "onthrow.method";

    String ON_INVOKE_INSTANCE_PARAMETER_KEY = "oninvoke.instance";

    String ON_RETURN_INSTANCE_PARAMETER_KEY = "onreturn.instance";

    String ON_THROW_INSTANCE_PARAMETER_KEY = "onthrow.instance";

    String ON_INVOKE_METHOD_ATTRIBUTE_KEY = "oninvoke-method";

    String ON_RETURN_METHOD_ATTRIBUTE_KEY = "onreturn-method";

    String ON_THROW_METHOD_ATTRIBUTE_KEY = "onthrow-method";

    String ON_INVOKE_INSTANCE_ATTRIBUTE_KEY = "oninvoke-instance";

    String ON_RETURN_INSTANCE_ATTRIBUTE_KEY = "onreturn-instance";

    String ON_THROW_INSTANCE_ATTRIBUTE_KEY = "onthrow-instance";


    // FIXME: is this still useful?
    String SHUTDOWN_TIMEOUT_KEY = "shutdown.timeout";


    String PROTOCOLS_SUFFIX = "dubbo.protocols.";


    String REGISTRIES_SUFFIX = "dubbo.registries.";

    String ZOOKEEPER_PROTOCOL = "zookeeper";

    String REGISTER_KEY = "register";

    String MULTI_SERIALIZATION_KEY = "serialize.multiple";

    String[] DOT_COMPATIBLE_KEYS = new String[]{QOS_ENABLE_COMPATIBLE, QOS_HOST_COMPATIBLE, QOS_PORT_COMPATIBLE,
        ACCEPT_FOREIGN_IP_COMPATIBLE, ACCEPT_FOREIGN_IP_WHITELIST_COMPATIBLE, REGISTRY_TYPE_KEY};

    String IGNORE_CHECK_KEYS = "ignoreCheckKeys";

    String PARAMETERS = "parameters";

    String SERVER_THREAD_POOL_NAME = "DubboServerHandler";

    String CLIENT_THREAD_POOL_NAME = "DubboClientHandler";
}
