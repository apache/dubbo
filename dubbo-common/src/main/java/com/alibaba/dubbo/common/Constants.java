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

package com.alibaba.dubbo.common;

import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

/**
 * Constants
 */
public class Constants {

    public static final String PROVIDER = "provider";

    public static final String CONSUMER = "consumer";

    public static final String REGISTER = "register";

    public static final String UNREGISTER = "unregister";

    public static final String SUBSCRIBE = "subscribe";

    public static final String UNSUBSCRIBE = "unsubscribe";

    public static final String CATEGORY_KEY = "category";

    public static final String PROVIDERS_CATEGORY = "providers";

    public static final String CONSUMERS_CATEGORY = "consumers";

    public static final String ROUTERS_CATEGORY = "routers";

    public static final String CONFIGURATORS_CATEGORY = "configurators";

    public static final String DEFAULT_CATEGORY = PROVIDERS_CATEGORY;

    public static final String ENABLED_KEY = "enabled";

    public static final String DISABLED_KEY = "disabled";

    public static final String VALIDATION_KEY = "validation";

    public static final String CACHE_KEY = "cache";

    public static final String DYNAMIC_KEY = "dynamic";

    public static final String DUBBO_PROPERTIES_KEY = "dubbo.properties.file";

    public static final String DEFAULT_DUBBO_PROPERTIES = "dubbo.properties";

    public static final String SENT_KEY = "sent";

    public static final boolean DEFAULT_SENT = false;

    public static final String REGISTRY_PROTOCOL = "registry";

    public static final String $INVOKE = "$invoke";

    public static final String $ECHO = "$echo";

    public static final int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    public static final String DEFAULT_PROXY = "javassist";

    public static final int DEFAULT_PAYLOAD = 8 * 1024 * 1024;                      // 8M

    public static final String DEFAULT_CLUSTER = "failover";

    public static final String DEFAULT_DIRECTORY = "dubbo";

    public static final String DEFAULT_LOADBALANCE = "random";

    public static final String DEFAULT_PROTOCOL = "dubbo";

    public static final String DEFAULT_EXCHANGER = "header";

    public static final String DEFAULT_TRANSPORTER = "netty";

    public static final String DEFAULT_REMOTING_SERVER = "netty";

    public static final String DEFAULT_REMOTING_CLIENT = "netty";

    public static final String DEFAULT_REMOTING_CODEC = "dubbo";

    public static final String DEFAULT_REMOTING_SERIALIZATION = "hessian2";

    public static final String DEFAULT_HTTP_SERVER = "servlet";

    public static final String DEFAULT_HTTP_CLIENT = "jdk";

    public static final String DEFAULT_HTTP_SERIALIZATION = "json";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final int DEFAULT_WEIGHT = 100;

    public static final int DEFAULT_FORKS = 2;

    public static final String DEFAULT_THREAD_NAME = "Dubbo";

    public static final int DEFAULT_CORE_THREADS = 0;

    public static final int DEFAULT_THREADS = 200;

    public static final int DEFAULT_QUEUES = 0;

    public static final int DEFAULT_ALIVE = 60 * 1000;

    public static final int DEFAULT_CONNECTIONS = 0;

    public static final int DEFAULT_ACCEPTS = 0;

    public static final int DEFAULT_IDLE_TIMEOUT = 600 * 1000;

    public static final int DEFAULT_HEARTBEAT = 60 * 1000;

    public static final int DEFAULT_TIMEOUT = 1000;

    public static final int DEFAULT_CONNECT_TIMEOUT = 3000;

    public static final int DEFAULT_RETRIES = 2;

    // default buffer size is 8k.
    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    public static final int MAX_BUFFER_SIZE = 16 * 1024;

    public static final int MIN_BUFFER_SIZE = 1 * 1024;

    public static final String REMOVE_VALUE_PREFIX = "-";

    public static final String HIDE_KEY_PREFIX = ".";

    public static final String DEFAULT_KEY_PREFIX = "default.";

    public static final String DEFAULT_KEY = "default";

    public static final String LOADBALANCE_KEY = "loadbalance";

    // key for router type, for e.g., "script"/"file",  corresponding to ScriptRouterFactory.NAME, FileRouterFactory.NAME 
    public static final String ROUTER_KEY = "router";

    public static final String CLUSTER_KEY = "cluster";

    public static final String REGISTRY_KEY = "registry";

    public static final String MONITOR_KEY = "monitor";

    public static final String SIDE_KEY = "side";

    public static final String PROVIDER_SIDE = "provider";

    public static final String CONSUMER_SIDE = "consumer";

    public static final String DEFAULT_REGISTRY = "dubbo";

    public static final String BACKUP_KEY = "backup";

    public static final String DIRECTORY_KEY = "directory";

    public static final String DEPRECATED_KEY = "deprecated";

    public static final String ANYHOST_KEY = "anyhost";

    public static final String ANYHOST_VALUE = "0.0.0.0";

    public static final String LOCALHOST_KEY = "localhost";

    public static final String LOCALHOST_VALUE = "127.0.0.1";

    public static final String APPLICATION_KEY = "application";

    public static final String LOCAL_KEY = "local";

    public static final String STUB_KEY = "stub";

    public static final String MOCK_KEY = "mock";

    public static final String PROTOCOL_KEY = "protocol";

    public static final String PROXY_KEY = "proxy";

    public static final String WEIGHT_KEY = "weight";

    public static final String FORKS_KEY = "forks";

    public static final String DEFAULT_THREADPOOL = "limited";

    public static final String DEFAULT_CLIENT_THREADPOOL = "cached";

    public static final String THREADPOOL_KEY = "threadpool";

    public static final String THREAD_NAME_KEY = "threadname";

    public static final String IO_THREADS_KEY = "iothreads";

    public static final String CORE_THREADS_KEY = "corethreads";

    public static final String THREADS_KEY = "threads";

    public static final String QUEUES_KEY = "queues";

    public static final String ALIVE_KEY = "alive";

    public static final String EXECUTES_KEY = "executes";

    public static final String BUFFER_KEY = "buffer";

    public static final String PAYLOAD_KEY = "payload";

    public static final String REFERENCE_FILTER_KEY = "reference.filter";

    public static final String INVOKER_LISTENER_KEY = "invoker.listener";

    public static final String SERVICE_FILTER_KEY = "service.filter";

    public static final String EXPORTER_LISTENER_KEY = "exporter.listener";

    public static final String ACCESS_LOG_KEY = "accesslog";

    public static final String ACTIVES_KEY = "actives";

    public static final String CONNECTIONS_KEY = "connections";

    public static final String ACCEPTS_KEY = "accepts";

    public static final String IDLE_TIMEOUT_KEY = "idle.timeout";

    public static final String HEARTBEAT_KEY = "heartbeat";

    public static final String HEARTBEAT_TIMEOUT_KEY = "heartbeat.timeout";

    public static final String CONNECT_TIMEOUT_KEY = "connect.timeout";

    public static final String TIMEOUT_KEY = "timeout";

    public static final String RETRIES_KEY = "retries";

    public static final String PROMPT_KEY = "prompt";

    public static final String DEFAULT_PROMPT = "dubbo>";

    public static final String CODEC_KEY = "codec";

    public static final String SERIALIZATION_KEY = "serialization";

    public static final String EXCHANGER_KEY = "exchanger";

    public static final String TRANSPORTER_KEY = "transporter";

    public static final String SERVER_KEY = "server";

    public static final String CLIENT_KEY = "client";

    public static final String ID_KEY = "id";

    public static final String ASYNC_KEY = "async";

    public static final String RETURN_KEY = "return";

    public static final String TOKEN_KEY = "token";

    public static final String METHOD_KEY = "method";

    public static final String METHODS_KEY = "methods";

    public static final String CHARSET_KEY = "charset";

    public static final String RECONNECT_KEY = "reconnect";

    public static final String SEND_RECONNECT_KEY = "send.reconnect";

    public static final int DEFAULT_RECONNECT_PERIOD = 2000;

    public static final String SHUTDOWN_TIMEOUT_KEY = "shutdown.timeout";

    public static final int DEFAULT_SHUTDOWN_TIMEOUT = 1000 * 60 * 15;

    public static final String PID_KEY = "pid";

    public static final String TIMESTAMP_KEY = "timestamp";

    public static final String REMOTE_TIMESTAMP_KEY = "remote.timestamp";

    public static final String WARMUP_KEY = "warmup";

    public static final int DEFAULT_WARMUP = 10 * 60 * 1000;

    public static final String CHECK_KEY = "check";

    public static final String REGISTER_KEY = "register";

    public static final String SUBSCRIBE_KEY = "subscribe";

    public static final String GROUP_KEY = "group";

    public static final String PATH_KEY = "path";

    public static final String INTERFACE_KEY = "interface";

    public static final String GENERIC_KEY = "generic";

    public static final String FILE_KEY = "file";

    public static final String DUMP_DIRECTORY = "dump.directory";

    public static final String WAIT_KEY = "wait";

    public static final String CLASSIFIER_KEY = "classifier";

    public static final String VERSION_KEY = "version";

    public static final String REVISION_KEY = "revision";

    public static final String DUBBO_VERSION_KEY = "dubbo";

    public static final String HESSIAN_VERSION_KEY = "hessian.version";

    public static final String DISPATCHER_KEY = "dispatcher";

    public static final String CHANNEL_HANDLER_KEY = "channel.handler";

    public static final String DEFAULT_CHANNEL_HANDLER = "default";

    public static final String ANY_VALUE = "*";

    public static final String COMMA_SEPARATOR = ",";

    public static final Pattern COMMA_SPLIT_PATTERN = Pattern
            .compile("\\s*[,]+\\s*");

    public final static String PATH_SEPARATOR = "/";

    public static final String REGISTRY_SEPARATOR = "|";

    public static final Pattern REGISTRY_SPLIT_PATTERN = Pattern
            .compile("\\s*[|;]+\\s*");

    public static final String SEMICOLON_SEPARATOR = ";";

    public static final Pattern SEMICOLON_SPLIT_PATTERN = Pattern
            .compile("\\s*[;]+\\s*");

    public static final String CONNECT_QUEUE_CAPACITY = "connect.queue.capacity";

    public static final String CONNECT_QUEUE_WARNING_SIZE = "connect.queue.warning.size";

    public static final int DEFAULT_CONNECT_QUEUE_WARNING_SIZE = 1000;

    public static final String CHANNEL_ATTRIBUTE_READONLY_KEY = "channel.readonly";

    public static final String CHANNEL_READONLYEVENT_SENT_KEY = "channel.readonly.sent";

    public static final String CHANNEL_SEND_READONLYEVENT_KEY = "channel.readonly.send";

    public static final String COUNT_PROTOCOL = "count";

    public static final String TRACE_PROTOCOL = "trace";

    public static final String EMPTY_PROTOCOL = "empty";

    public static final String ADMIN_PROTOCOL = "admin";

    public static final String PROVIDER_PROTOCOL = "provider";

    public static final String CONSUMER_PROTOCOL = "consumer";

    public static final String ROUTE_PROTOCOL = "route";

    public static final String SCRIPT_PROTOCOL = "script";

    public static final String CONDITION_PROTOCOL = "condition";

    public static final String MOCK_PROTOCOL = "mock";

    public static final String RETURN_PREFIX = "return ";

    public static final String THROW_PREFIX = "throw";

    public static final String FAIL_PREFIX = "fail:";

    public static final String FORCE_PREFIX = "force:";

    public static final String FORCE_KEY = "force";

    public static final String MERGER_KEY = "merger";

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

    /**
     * To decide whether to make connection when the client is created
     */
    public static final String LAZY_CONNECT_KEY = "lazy";

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

    /**
     * To decide whether register center saves file synchronously, the default value is asynchronously
     */
    public static final String REGISTRY_FILESAVE_SYNC_KEY = "save.file";

    /**
     * Period of registry center's retry interval
     */
    public static final String REGISTRY_RETRY_PERIOD_KEY = "retry.period";

    /**
     * Default value for the period of retry interval in milliseconds: 5000
     */
    public static final int DEFAULT_REGISTRY_RETRY_PERIOD = 5 * 1000;

    /**
     * Reconnection period in milliseconds for register center
     */
    public static final String REGISTRY_RECONNECT_PERIOD_KEY = "reconnect.period";

    public static final int DEFAULT_REGISTRY_RECONNECT_PERIOD = 3 * 1000;

    public static final String SESSION_TIMEOUT_KEY = "session";

    public static final int DEFAULT_SESSION_TIMEOUT = 60 * 1000;

    /**
     * The key name for export URL in register center
     */
    public static final String EXPORT_KEY = "export";

    /**
     * The key name for reference URL in register center
     */
    public static final String REFER_KEY = "refer";

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

    @Deprecated
    public static final String SHUTDOWN_WAIT_SECONDS_KEY = "dubbo.service.shutdown.wait.seconds";

    public static final String SHUTDOWN_WAIT_KEY = "dubbo.service.shutdown.wait";

    public static final String IS_SERVER_KEY = "isserver";

    /**
     * Default timeout value in milliseconds for server shutdown
     */
    public static final int DEFAULT_SERVER_SHUTDOWN_TIMEOUT = 10000;

    public static final String ON_CONNECT_KEY = "onconnect";

    public static final String ON_DISCONNECT_KEY = "ondisconnect";

    public static final String ON_INVOKE_METHOD_KEY = "oninvoke.method";

    public static final String ON_RETURN_METHOD_KEY = "onreturn.method";

    public static final String ON_THROW_METHOD_KEY = "onthrow.method";

    public static final String ON_INVOKE_INSTANCE_KEY = "oninvoke.instance";

    public static final String ON_RETURN_INSTANCE_KEY = "onreturn.instance";

    public static final String ON_THROW_INSTANCE_KEY = "onthrow.instance";

    public static final String OVERRIDE_PROTOCOL = "override";

    public static final String PRIORITY_KEY = "priority";

    public static final String RULE_KEY = "rule";

    public static final String TYPE_KEY = "type";

    public static final String RUNTIME_KEY = "runtime";

    /**
     * when ROUTER_KEY's value is set to ROUTER_TYPE_CLEAR, RegistryDirectory will clean all current routers
     */
    public static final String ROUTER_TYPE_CLEAR = "clean";

    public static final String DEFAULT_SCRIPT_TYPE_KEY = "javascript";

    public static final String STUB_EVENT_KEY = "dubbo.stub.event";

    public static final boolean DEFAULT_STUB_EVENT = false;

    public static final String STUB_EVENT_METHODS_KEY = "dubbo.stub.event.methods";

    /**
     * When this attribute appears in invocation's attachment, mock invoker will be used
     */
    public static final String INVOCATION_NEED_MOCK = "invocation.need.mock";

    public static final String LOCAL_PROTOCOL = "injvm";

    public static final String AUTO_ATTACH_INVOCATIONID_KEY = "invocationid.autoattach";

    public static final String SCOPE_KEY = "scope";

    public static final String SCOPE_LOCAL = "local";

    public static final String SCOPE_REMOTE = "remote";

    public static final String SCOPE_NONE = "none";

    public static final String RELIABLE_PROTOCOL = "napoli";

    public static final String TPS_LIMIT_RATE_KEY = "tps";

    public static final String TPS_LIMIT_INTERVAL_KEY = "tps.interval";

    public static final long DEFAULT_TPS_LIMIT_INTERVAL = 60 * 1000;

    public static final String DECODE_IN_IO_THREAD_KEY = "decode.in.io";

    public static final boolean DEFAULT_DECODE_IN_IO_THREAD = true;

    public static final String INPUT_KEY = "input";

    public static final String OUTPUT_KEY = "output";

    public static final String EXECUTOR_SERVICE_COMPONENT_KEY = ExecutorService.class.getName();

    public static final String GENERIC_SERIALIZATION_NATIVE_JAVA = "nativejava";

    public static final String GENERIC_SERIALIZATION_DEFAULT = "true";

    public static final String GENERIC_SERIALIZATION_BEAN = "bean";

    public static final String DUBBO_IP_TO_REGISTRY = "DUBBO_IP_TO_REGISTRY";

    public static final String DUBBO_PORT_TO_REGISTRY = "DUBBO_PORT_TO_REGISTRY";

    public static final String DUBBO_IP_TO_BIND = "DUBBO_IP_TO_BIND";

    public static final String DUBBO_PORT_TO_BIND = "DUBBO_PORT_TO_BIND";

    public static final String BIND_IP_KEY = "bind.ip";

    public static final String BIND_PORT_KEY = "bind.port";

    public static final String REGISTER_IP_KEY = "register.ip";

    /*
     * private Constants(){ }
     */

}
