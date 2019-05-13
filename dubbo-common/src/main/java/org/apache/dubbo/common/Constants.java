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

package org.apache.dubbo.common;


import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;

/**
 * Constants
 */
public class Constants {




    // BEGIN dubbo-config-api
    public static final String CLUSTER_KEY = "cluster";

    public static final String STATUS_KEY = "status";

    public static final String CONTEXTPATH_KEY = "contextpath";

    public static final String LISTENER_KEY = "listener";

    public static final String LAYER_KEY = "layer";

    /**
     * General
     */
    /**
     * Application name;
     */
    public static final String NAME = "name";

    /**
     * Application owner name;
     */
    public static final String OWNER = "owner";

    /**
     * Running application organization name.
     */
    public static final String ORGANIZATION = "organization";

    /**
     * Application architecture name.
     */
    public static final String ARCHITECTURE = "architecture";

    /**
     * Environment name
     */
    public static final String ENVIRONMENT = "environment";

    /**
     * Test environment key.
     */
    public static final String TEST_ENVIRONMENT = "test";

    /**
     * Development environment key.
     */
    public static final String DEVELOPMENT_ENVIRONMENT = "develop";

    /**
     * Production environment key.
     */
    public static final String PRODUCTION_ENVIRONMENT = "product";

    public static final String CONFIG_CLUSTER_KEY = "config.cluster";
    public static final String CONFIG_NAMESPACE_KEY = "config.namespace";
    public static final String CONFIG_GROUP_KEY = "config.group";
    public static final String CONFIG_CHECK_KEY = "config.check";

    public static final String CONFIG_CONFIGFILE_KEY = "config.config-file";
    public static final String CONFIG_ENABLE_KEY = "config.highest-priority";
    public static final String CONFIG_TIMEOUT_KEY = "config.timeout";
    public static final String CONFIG_APPNAME_KEY = "config.app-name";

    public static final String USERNAME_KEY = "username";

    public static final String PASSWORD_KEY = "password";

    public static final String HOST_KEY = "host";

    public static final String PORT_KEY = "port";

    public static final String MULTICAST = "multicast";

    public static final String REGISTER_IP_KEY = "register.ip";

    public static final String DUBBO_IP_TO_REGISTRY = "DUBBO_IP_TO_REGISTRY";

    public static final String DUBBO_PORT_TO_REGISTRY = "DUBBO_PORT_TO_REGISTRY";

    public static final String DUBBO_IP_TO_BIND = "DUBBO_IP_TO_BIND";

    public static final String DUBBO_PORT_TO_BIND = "DUBBO_PORT_TO_BIND";

    public static final String SCOPE_KEY = "scope";

    public static final String SCOPE_LOCAL = "local";

    public static final String SCOPE_REMOTE = "remote";

    public static final String SCOPE_NONE = "none";

    public static final String ON_CONNECT_KEY = "onconnect";

    public static final String ON_DISCONNECT_KEY = "ondisconnect";

    public static final String ON_INVOKE_METHOD_KEY = "oninvoke.method";

    public static final String ON_RETURN_METHOD_KEY = "onreturn.method";

    public static final String ON_THROW_METHOD_KEY = "onthrow.method";

    public static final String ON_INVOKE_INSTANCE_KEY = "oninvoke.instance";

    public static final String ON_RETURN_INSTANCE_KEY = "onreturn.instance";

    public static final String ON_THROW_INSTANCE_KEY = "onthrow.instance";

    @Deprecated
    public static final String SHUTDOWN_WAIT_SECONDS_KEY = "dubbo.service.shutdown.wait.seconds";

    public static final String SHUTDOWN_WAIT_KEY = "dubbo.service.shutdown.wait";

    /**
     * The key name for export URL in register center
     */
    public static final String EXPORT_KEY = "export";

    /**
     * The key name for reference URL in register center
     */
    public static final String REFER_KEY = "refer";

    /**
     * To decide whether to make connection when the client is created
     */
    public static final String LAZY_CONNECT_KEY = "lazy";

    public static final String DUBBO_PROTOCOL = DUBBO;

    public static final String ZOOKEEPER_PROTOCOL = "zookeeper";

    // FIXME: is this still useful?
    public static final String SHUTDOWN_TIMEOUT_KEY = "shutdown.timeout";

    public static final int DEFAULT_SHUTDOWN_TIMEOUT = 1000 * 60 * 15;

    public static final String PROTOCOLS_SUFFIX = "dubbo.protocols.";

    public static final String PROTOCOL_SUFFIX = "dubbo.protocol.";

    public static final String REGISTRIES_SUFFIX = "dubbo.registries.";

    public static final String TELNET = "telnet";

    public static final String QOS_ENABLE = "qos.enable";

    public static final String QOS_PORT = "qos.port";

    public static final String ACCEPT_FOREIGN_IP = "qos.accept.foreign.ip";
    // END dubbo-congfig-api

    // BEGIN dubbo-cluster
    /**
     * key for router type, for e.g., "script"/"file",  corresponding to ScriptRouterFactory.NAME, FileRouterFactory.NAME
     */
    public static final String ROUTER_KEY = "router";

    public static final String LOADBALANCE_KEY = "loadbalance";

    public static final String DEFAULT_LOADBALANCE = "random";

    public static final String FAIL_BACK_TASKS_KEY = "failbacktasks";

    public static final int DEFAULT_FAILBACK_TASKS = 100;

    public static final String RETRIES_KEY = "retries";

    public static final int DEFAULT_RETRIES = 2;

    public static final int DEFAULT_FAILBACK_TIMES = 3;

    public static final String FORKS_KEY = "forks";

    public static final int DEFAULT_FORKS = 2;

    public static final String WEIGHT_KEY = "weight";

    public static final int DEFAULT_WEIGHT = 100;

    public static final String MOCK_PROTOCOL = "mock";

    public static final String FORCE_KEY = "force";

    /**
     * To decide whether to exclude unavailable invoker from the cluster
     */
    public static final String CLUSTER_AVAILABLE_CHECK_KEY = "cluster.availablecheck";

    /**
     * The default value of cluster.availablecheck
     *
     * @see #CLUSTER_AVAILABLE_CHECK_KEY
     */
    public static final boolean DEFAULT_CLUSTER_AVAILABLE_CHECK = true;

    /**
     * To decide whether to enable sticky strategy for cluster
     */
    public static final String CLUSTER_STICKY_KEY = "sticky";

    /**
     * The default value of sticky
     *
     * @see #CLUSTER_STICKY_KEY
     */
    public static final boolean DEFAULT_CLUSTER_STICKY = false;

    public static final String ADDRESS_KEY = "address";

    /**
     * When this attribute appears in invocation's attachment, mock invoker will be used
     */
    public static final String INVOCATION_NEED_MOCK = "invocation.need.mock";

    /**
     * when ROUTER_KEY's value is set to ROUTER_TYPE_CLEAR, RegistryDirectory will clean all current routers
     */
    public static final String ROUTER_TYPE_CLEAR = "clean";

    public static final String DEFAULT_SCRIPT_TYPE_KEY = "javascript";

    public static final String PRIORITY_KEY = "priority";

    public static final String RULE_KEY = "rule";

    public static final String TYPE_KEY = "type";

    public static final String RUNTIME_KEY = "runtime";

    public static final String TAG_KEY = "dubbo.tag";

    public static final String REMOTE_TIMESTAMP_KEY = "remote.timestamp";

    public static final String WARMUP_KEY = "warmup";

    public static final int DEFAULT_WARMUP = 10 * 60 * 1000;

    public static final String CONFIG_VERSION_KEY = "configVersion";

    public static final String OVERRIDE_PROVIDERS_KEY = "providerAddresses";
    // END dubbo-cluster


    // BEGIN dubbo-monitor-api
    public static final String MONITOR_KEY = "monitor";
    public static final String LOGSTAT_PROTOCOL = "logstat";
    public static final String COUNT_PROTOCOL = "count";
    public static final String DUBBO_PROVIDER = "dubbo.provider";
    public static final String DUBBO_CONSUMER = "dubbo.consumer";
    public static final String DUBBO_PROVIDER_METHOD = "dubbo.provider.method";
    public static final String DUBBO_CONSUMER_METHOD = "dubbo.consumer.method";
    public static final String SERVICE = "service";
    public static final String METHOD = "method";
    public static final String DUBBO_GROUP = "dubbo";
    public static final String METRICS_KEY = "metrics";
    public static final String METRICS_PORT = "metrics.port";
    public static final String METRICS_PROTOCOL = "metrics.protocol";
    // END dubbo-monitor-api

    // BEGIN dubbo-metadata-report-api
    public static final String METADATA_REPORT_KEY = "metadata";

    public static final String RETRY_TIMES_KEY = "retry.times";

    public static final Integer DEFAULT_METADATA_REPORT_RETRY_TIMES = 100;

    public static final String RETRY_PERIOD_KEY = "retry.period";

    public static final Integer DEFAULT_METADATA_REPORT_RETRY_PERIOD = 3000;

    public static final String SYNC_REPORT_KEY = "sync.report";

    public static final String CYCLE_REPORT_KEY = "cycle.report";

    public static final Boolean DEFAULT_METADATA_REPORT_CYCLE_REPORT = true;
    // END dubbo-metadata-report-api

    // BEGIN dubbo-filter-cache
    public static final String CACHE_KEY = "cache";
    // END dubbo-filter-cache


    // BEGIN dubbo-filter-validation
    public static final String VALIDATION_KEY = "validation";
    // END dubbo-filter-validation

    public static final String DEFAULT_CLUSTER = "failover";

    /**
     * public static final int DEFAULT_REGISTRY_CONNECT_TIMEOUT = 5000;
     */

    public static final String DIRECTORY_KEY = "directory";

    public static final String ASYNC_SUFFIX = "Async";

    public static final String WAIT_KEY = "wait";

    public static final String HESSIAN_VERSION_KEY = "hessian.version";


    public static final String CONFIG_PROTOCOL = "config";


    public static final String RELIABLE_PROTOCOL = "napoli";
}
