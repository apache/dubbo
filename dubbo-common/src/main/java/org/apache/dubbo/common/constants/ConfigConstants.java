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
    String CLUSTER_KEY = "cluster";

    String CONFIG_CLUSTER_KEY = "config.cluster";
    String CONFIG_NAMESPACE_KEY = "config.namespace";
    String CONFIG_GROUP_KEY = "config.group";
    String CONFIG_CHECK_KEY = "config.check";

    String USERNAME_KEY = "username";

    String PASSWORD_KEY = "password";

    String HOST_KEY = "host";

    String PORT_KEY = "port";

    String REGISTER_IP_KEY = "register.ip";

    String DUBBO_IP_TO_BIND = "DUBBO_IP_TO_BIND";

    String SCOPE_KEY = "scope";

    String SCOPE_LOCAL = "local";

    String SCOPE_REMOTE = "remote";

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

    String TELNET = "telnet";

    String QOS_ENABLE = "qos.enable";

    String QOS_PORT = "qos.port";

    String ACCEPT_FOREIGN_IP = "qos.accept.foreign.ip";
}
