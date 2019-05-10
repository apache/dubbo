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
 * ConfigConstants
 */
public interface ConfigConstants {
    // BEGIN dubbo-config-api
    String CLUSTER_KEY = "cluster";

    String STATUS_KEY = "status";

    String CONTEXTPATH_KEY = "contextpath";

    String LISTENER_KEY = "listener";

    String LAYER_KEY = "layer";

    /**
     * General
     */
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

    String CONFIG_CLUSTER_KEY = "config.cluster";
    String CONFIG_NAMESPACE_KEY = "config.namespace";
    String CONFIG_GROUP_KEY = "config.group";
    String CONFIG_CHECK_KEY = "config.check";

    String CONFIG_CONFIGFILE_KEY = "config.config-file";
    String CONFIG_ENABLE_KEY = "config.highest-priority";
    String CONFIG_TIMEOUT_KEY = "config.timeout";
    String CONFIG_APPNAME_KEY = "config.app-name";

    String USERNAME_KEY = "username";

    String PASSWORD_KEY = "password";

    String HOST_KEY = "host";

    String PORT_KEY = "port";

    String MULTICAST = "multicast";

    String REGISTER_IP_KEY = "register.ip";

    String DUBBO_IP_TO_REGISTRY = "DUBBO_IP_TO_REGISTRY";

    String DUBBO_PORT_TO_REGISTRY = "DUBBO_PORT_TO_REGISTRY";

    String DUBBO_IP_TO_BIND = "DUBBO_IP_TO_BIND";

    String DUBBO_PORT_TO_BIND = "DUBBO_PORT_TO_BIND";

    String SCOPE_KEY = "scope";

    String SCOPE_LOCAL = "local";

    String SCOPE_REMOTE = "remote";

    String SCOPE_NONE = "none";

    String ON_CONNECT_KEY = "onconnect";

    String ON_DISCONNECT_KEY = "ondisconnect";

    String ON_INVOKE_METHOD_KEY = "oninvoke.method";

    String ON_RETURN_METHOD_KEY = "onreturn.method";

    String ON_THROW_METHOD_KEY = "onthrow.method";

    String ON_INVOKE_INSTANCE_KEY = "oninvoke.instance";

    String ON_RETURN_INSTANCE_KEY = "onreturn.instance";

    String ON_THROW_INSTANCE_KEY = "onthrow.instance";

    @Deprecated
    String SHUTDOWN_WAIT_SECONDS_KEY = "dubbo.service.shutdown.wait.seconds";

    String SHUTDOWN_WAIT_KEY = "dubbo.service.shutdown.wait";

    /**
     * The key name for export URL in register center
     */
    String EXPORT_KEY = "export";

    /**
     * The key name for reference URL in register center
     */
    String REFER_KEY = "refer";

    /**
     * To decide whether to make connection when the client is created
     */
    String LAZY_CONNECT_KEY = "lazy";

    String DUBBO_PROTOCOL = "dubbo";

    String ZOOKEEPER_PROTOCOL = "zookeeper";

    // FIXME: is this still useful?
    String SHUTDOWN_TIMEOUT_KEY = "shutdown.timeout";

    int DEFAULT_SHUTDOWN_TIMEOUT = 1000 * 60 * 15;

    String PROTOCOLS_SUFFIX = "dubbo.protocols.";

    String PROTOCOL_SUFFIX = "dubbo.protocol.";

    String REGISTRIES_SUFFIX = "dubbo.registries.";

    String TELNET = "telnet";

    String QOS_ENABLE = "qos.enable";

    String QOS_PORT = "qos.port";

    String ACCEPT_FOREIGN_IP = "qos.accept.foreign.ip";
    // END dubbo-congfig-api
}
