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

import org.apache.dubbo.common.URL;

import java.net.NetworkInterface;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public interface CommonConstants {

    String DUBBO = "dubbo";

    String TRIPLE = "tri";

    String PROVIDER = "provider";

    String CONSUMER = "consumer";

    String CALLBACK = "callback";

    String APPLICATION_KEY = "application";

    String APPLICATION_VERSION_KEY = "application.version";

    String APPLICATION_PROTOCOL_KEY = "application-protocol";

    String METADATA_SERVICE_PORT_KEY = "metadata-service-port";

    String METADATA_SERVICE_PROTOCOL_KEY = "metadata-service-protocol";

    String METRICS_SERVICE_PORT_KEY = "metrics-service-port";

    String METRICS_SERVICE_PROTOCOL_KEY = "metrics-service-protocol";

    String LIVENESS_PROBE_KEY = "liveness-probe";

    String READINESS_PROBE_KEY = "readiness-probe";

    String STARTUP_PROBE = "startup-probe";

    String REMOTE_APPLICATION_KEY = "remote.application";

    String ENABLED_KEY = "enabled";

    String DISABLED_KEY = "disabled";

    String DUBBO_PROPERTIES_KEY = "dubbo.properties.file";

    String DEFAULT_DUBBO_PROPERTIES = "dubbo.properties";

    String DUBBO_MIGRATION_KEY = "dubbo.migration.file";

    String DUBBO_MIGRATION_FILE_ENABLE = "dubbo.migration-file.enable";

    String DEFAULT_DUBBO_MIGRATION_FILE = "dubbo-migration.yaml";

    String ANY_VALUE = "*";

    /**
     * @since 2.7.8
     */
    char COMMA_SEPARATOR_CHAR = ',';

    String COMMA_SEPARATOR = ",";

    String DOT_SEPARATOR = ".";

    Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

    String PATH_SEPARATOR = "/";

    String PROTOCOL_SEPARATOR = "://";

    String PROTOCOL_SEPARATOR_ENCODED = URL.encode(PROTOCOL_SEPARATOR);

    String REGISTRY_SEPARATOR = "|";

    Pattern REGISTRY_SPLIT_PATTERN = Pattern.compile("\\s*[|;]+\\s*");

    Pattern D_REGISTRY_SPLIT_PATTERN = Pattern.compile("\\s*[|]+\\s*");

    String SEMICOLON_SEPARATOR = ";";

    Pattern SEMICOLON_SPLIT_PATTERN = Pattern.compile("\\s*[;]+\\s*");

    Pattern EQUAL_SPLIT_PATTERN = Pattern.compile("\\s*[=]+\\s*");

    String DEFAULT_PROXY = "javassist";

    String DEFAULT_DIRECTORY = "dubbo";

    String PROTOCOL_KEY = "protocol";

    String DEFAULT_PROTOCOL = "dubbo";

    String DEFAULT_THREAD_NAME = "Dubbo";

    int DEFAULT_CORE_THREADS = 0;

    int DEFAULT_THREADS = 200;

    String EXECUTOR_SERVICE_COMPONENT_KEY = ExecutorService.class.getName();

    String CONSUMER_SHARED_EXECUTOR_SERVICE_COMPONENT_KEY = "CONSUMER_SHARED_SERVICE_EXECUTOR";

    String THREADPOOL_KEY = "threadpool";

    String THREAD_NAME_KEY = "threadname";

    String CORE_THREADS_KEY = "corethreads";

    String THREAD_POOL_EXHAUSTED_LISTENERS_KEY = "thread-pool-exhausted-listeners";

    String THREADS_KEY = "threads";

    String QUEUES_KEY = "queues";

    String ALIVE_KEY = "alive";

    String DEFAULT_THREADPOOL = "limited";

    String DEFAULT_CLIENT_THREADPOOL = "cached";

    String IO_THREADS_KEY = "iothreads";

    String KEEP_ALIVE_KEY = "keep.alive";

    int DEFAULT_QUEUES = 0;

    int DEFAULT_ALIVE = 60 * 1000;

    String TIMEOUT_KEY = "timeout";

    int DEFAULT_TIMEOUT = 1000;

    String SESSION_KEY = "session";

    // used by invocation attachments to transfer timeout from Consumer to Provider.
    // works as a replacement of TIMEOUT_KEY on wire, which seems to be totally useless in previous releases).
    String TIMEOUT_ATTACHMENT_KEY = "_TO";

    String TIMEOUT_ATTACHMENT_KEY_LOWER = "_to";

    String TIME_COUNTDOWN_KEY = "timeout-countdown";

    String ENABLE_TIMEOUT_COUNTDOWN_KEY = "enable-timeout-countdown";

    String REMOVE_VALUE_PREFIX = "-";

    String PROPERTIES_CHAR_SEPARATOR = "-";

    String UNDERLINE_SEPARATOR = "_";

    String SEPARATOR_REGEX = "_|-";

    String GROUP_CHAR_SEPARATOR = ":";

    String HIDE_KEY_PREFIX = ".";

    String DOT_REGEX = "\\.";

    String DEFAULT_KEY_PREFIX = "default.";

    String DEFAULT_KEY = "default";

    String PREFERRED_KEY = "preferred";

    /**
     * Default timeout value in milliseconds for server shutdown
     */
    int DEFAULT_SERVER_SHUTDOWN_TIMEOUT = 10000;

    String SIDE_KEY = "side";

    String PROVIDER_SIDE = "provider";

    String CONSUMER_SIDE = "consumer";

    String ANYHOST_KEY = "anyhost";

    String ANYHOST_VALUE = "0.0.0.0";

    String LOCALHOST_KEY = "localhost";

    String LOCALHOST_VALUE = "127.0.0.1";

    String METHODS_KEY = "methods";

    String METHOD_KEY = "method";

    String PID_KEY = "pid";

    String TIMESTAMP_KEY = "timestamp";

    String GROUP_KEY = "group";

    String PATH_KEY = "path";

    String ADDRESS_KEY = "address";

    String INTERFACE_KEY = "interface";

    String FILE_KEY = "file";

    String FILTER_KEY = "filter";

    String DUMP_DIRECTORY = "dump.directory";

    String DUMP_ENABLE = "dump.enable";

    String CLASSIFIER_KEY = "classifier";

    String VERSION_KEY = "version";

    String REVISION_KEY = "revision";

    String METADATA_KEY = "metadata-type";

    String REPORT_METADATA_KEY = "report-metadata";

    String REPORT_DEFINITION_KEY = "report-definition";

    String DEFAULT_METADATA_STORAGE_TYPE = "local";

    String REMOTE_METADATA_STORAGE_TYPE = "remote";

    String INTERFACE_REGISTER_MODE = "interface";

    String INSTANCE_REGISTER_MODE = "instance";

    String DEFAULT_REGISTER_MODE = "all";

    String GENERIC_KEY = "generic";

    /**
     * The composite metadata storage type includes {@link #DEFAULT_METADATA_STORAGE_TYPE "local"} and
     * {@link #REMOTE_METADATA_STORAGE_TYPE "remote"}.
     *
     * @since 2.7.8
     */
    String COMPOSITE_METADATA_STORAGE_TYPE = "composite";

    /**
     * Consumer side 's proxy class
     */
    String PROXY_CLASS_REF = "refClass";

    /**
     * generic call
     */
    String $INVOKE = "$invoke";

    String $INVOKE_ASYNC = "$invokeAsync";

    String GENERIC_PARAMETER_DESC = "Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/Object;";

    /**
     * echo call
     */
    String $ECHO = "$echo";

    /**
     * package version in the manifest
     */
    String RELEASE_KEY = "release";

    String PROTOBUF_MESSAGE_CLASS_NAME = "com.google.protobuf.Message";

    int MAX_PROXY_COUNT = 65535;

    String MONITOR_KEY = "monitor";

    String BACKGROUND_KEY = "background";

    String CLUSTER_KEY = "cluster";

    String USERNAME_KEY = "username";

    String PASSWORD_KEY = "password";

    String HOST_KEY = "host";

    String PORT_KEY = "port";

    String DUBBO_IP_TO_BIND = "DUBBO_IP_TO_BIND";

    /**
     * broadcast cluster.
     */
    String BROADCAST_CLUSTER = "broadcast";

    /**
     * The property name for {@link NetworkInterface#getDisplayName() the name of network interface} that
     * the Dubbo application prefers
     *
     * @since 2.7.6
     */
    String DUBBO_PREFERRED_NETWORK_INTERFACE = "dubbo.network.interface.preferred";

    @Deprecated
    String SHUTDOWN_WAIT_SECONDS_KEY = "dubbo.service.shutdown.wait.seconds";

    String SHUTDOWN_WAIT_KEY = "dubbo.service.shutdown.wait";

    String DUBBO_PROTOCOL = "dubbo";

    String DUBBO_LABELS = "dubbo.labels";

    String DUBBO_ENV_KEYS = "dubbo.env.keys";

    String CONFIG_CONFIGFILE_KEY = "config-file";

    String CONFIG_ENABLE_KEY = "highest-priority";

    String CONFIG_NAMESPACE_KEY = "namespace";

    String CHECK_KEY = "check";

    String BACKLOG_KEY = "backlog";

    String HEARTBEAT_EVENT = null;

    String MOCK_HEARTBEAT_EVENT = "H";

    String READONLY_EVENT = "R";

    String WRITEABLE_EVENT = "W";

    String REFERENCE_FILTER_KEY = "reference.filter";

    String HEADER_FILTER_KEY = "header.filter";

    String INVOCATION_INTERCEPTOR_KEY = "invocation.interceptor";

    String INVOKER_LISTENER_KEY = "invoker.listener";

    String REGISTRY_PROTOCOL_LISTENER_KEY = "registry.protocol.listener";

    String DUBBO_VERSION_KEY = "dubbo";

    String TAG_KEY = "dubbo.tag";

    /**
     * To decide whether to make connection when the client is created
     */
    String LAZY_CONNECT_KEY = "lazy";

    String STUB_EVENT_KEY = "dubbo.stub.event";

    String REFERENCE_INTERCEPTOR_KEY = "reference.interceptor";

    String SERVICE_FILTER_KEY = "service.filter";

    String EXPORTER_LISTENER_KEY = "exporter.listener";

    /**
     * After simplify the registry, should add some parameter individually for provider.
     *
     * @since 2.7.0
     */
    String EXTRA_KEYS_KEY = "extra-keys";

    String GENERIC_SERIALIZATION_NATIVE_JAVA = "nativejava";

    String GENERIC_SERIALIZATION_GSON = "gson";

    String GENERIC_SERIALIZATION_DEFAULT = "true";

    String GENERIC_SERIALIZATION_BEAN = "bean";

    String GENERIC_RAW_RETURN = "raw.return";

    String GENERIC_SERIALIZATION_PROTOBUF = "protobuf-json";

    String GENERIC_WITH_CLZ_KEY = "generic.include.class";

    /**
     * Whether to cache locally, default is true
     */
    String REGISTRY_LOCAL_FILE_CACHE_ENABLED = "file-cache";

    String METADATA_INFO_CACHE_EXPIRE_KEY = "metadata-info-cache.expire";

    int DEFAULT_METADATA_INFO_CACHE_EXPIRE = 10 * 60 * 1000;

    String METADATA_INFO_CACHE_SIZE_KEY = "metadata-info-cache.size";

    int DEFAULT_METADATA_INFO_CACHE_SIZE = 16;

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

    String LOADBALANCE_KEY = "loadbalance";

    String DEFAULT_LOADBALANCE = "random";

    String RETRIES_KEY = "retries";

    String FORKS_KEY = "forks";

    int DEFAULT_RETRIES = 2;

    int DEFAULT_FAILBACK_TIMES = 3;

    String INTERFACES = "interfaces";

    String SSL_ENABLED_KEY = "ssl-enabled";

    String SERVICE_PATH_PREFIX = "service.path.prefix";

    String PROTOCOL_SERVER_SERVLET = "servlet";

    String PROTOCOL_SERVER = "server";

    String IPV6_KEY = "ipv6";

    /**
     * The parameter key for the class path of the ServiceNameMapping {@link Properties} file
     *
     * @since 2.7.8
     */
    String SERVICE_NAME_MAPPING_PROPERTIES_FILE_KEY = "service-name-mapping.properties-path";

    /**
     * The default class path of the ServiceNameMapping {@link Properties} file
     *
     * @since 2.7.8
     */
    String DEFAULT_SERVICE_NAME_MAPPING_PROPERTIES_PATH = "META-INF/dubbo/service-name-mapping.properties";

    String CLASS_DESERIALIZE_BLOCK_ALL = "dubbo.security.serialize.blockAllClassExceptAllow";

    String CLASS_DESERIALIZE_ALLOWED_LIST = "dubbo.security.serialize.allowedClassList";

    String CLASS_DESERIALIZE_BLOCKED_LIST = "dubbo.security.serialize.blockedClassList";

    String ENABLE_NATIVE_JAVA_GENERIC_SERIALIZE = "dubbo.security.serialize.generic.native-java-enable";

    String SERIALIZE_BLOCKED_LIST_FILE_PATH = "security/serialize.blockedlist";

    String SERIALIZE_ALLOW_LIST_FILE_PATH = "security/serialize.allowlist";

    String SERIALIZE_CHECK_STATUS_KEY = "dubbo.application.serialize-check-status";

    String QOS_LIVE_PROBE_EXTENSION = "dubbo.application.liveness-probe";

    String QOS_READY_PROBE_EXTENSION = "dubbo.application.readiness-probe";

    String QOS_STARTUP_PROBE_EXTENSION = "dubbo.application.startup-probe";

    String REGISTRY_DELAY_NOTIFICATION_KEY = "delay-notification";

    String CACHE_CLEAR_TASK_INTERVAL = "dubbo.application.url.cache.task.interval";

    String CACHE_CLEAR_WAITING_THRESHOLD = "dubbo.application.url.cache.clear.waiting";

    String CLUSTER_INTERCEPTOR_COMPATIBLE_KEY = "dubbo.application.cluster.interceptor.compatible";

    String UTF8ENCODE = "UTF-8";

    /**
     * Pseudo URL prefix for loading from the class path: "classpath:".
     */
    String CLASSPATH_URL_PREFIX = "classpath:";

    String DEFAULT_VERSION = "0.0.0";

    String CLASS_DESERIALIZE_OPEN_CHECK = "dubbo.security.serialize.openCheckClass";

    String ROUTER_KEY = "router";

    String EXPORT_ASYNC_KEY = "export-async";

    String REFER_ASYNC_KEY = "refer-async";

    String EXPORT_BACKGROUND_KEY = "export-background";

    String REFER_BACKGROUND_KEY = "refer-background";

    String EXPORT_THREAD_NUM_KEY = "export-thread-num";

    String REFER_THREAD_NUM_KEY = "refer-thread-num";

    int DEFAULT_EXPORT_THREAD_NUM = 10;

    int DEFAULT_REFER_THREAD_NUM = 10;

    int DEFAULT_DELAY_NOTIFICATION_TIME = 5000;

    int DEFAULT_DELAY_EXECUTE_TIMES = 10;

    /**
     * Url merge processor key
     */
    String URL_MERGE_PROCESSOR_KEY = "url-merge-processor";

    /**
     * use native image to compile dubbo's identifier
     */
    String NATIVE = "native";

    String DUBBO_MONITOR_ADDRESS = "dubbo.monitor.address";

    String SERVICE_NAME_MAPPING_KEY = "service-name-mapping";

    String SCOPE_MODEL = "scopeModel";

    String SERVICE_MODEL = "serviceModel";

    /**
     * The property name for {@link NetworkInterface#getDisplayName() the name of network interface} that
     * the Dubbo application will be ignored
     *
     * @since 2.7.6
     */
    String DUBBO_NETWORK_IGNORED_INTERFACE = "dubbo.network.interface.ignored";

    String OS_NAME_KEY = "os.name";

    String OS_LINUX_PREFIX = "linux";

    String OS_WIN_PREFIX = "win";

    String RECONNECT_TASK_TRY_COUNT = "dubbo.reconnect.reconnectTaskTryCount";

    int DEFAULT_RECONNECT_TASK_TRY_COUNT = 10;

    String RECONNECT_TASK_PERIOD = "dubbo.reconnect.reconnectTaskPeriod";

    int DEFAULT_RECONNECT_TASK_PERIOD = 1000;

    String RESELECT_COUNT = "dubbo.reselect.count";

    int DEFAULT_RESELECT_COUNT = 10;

    String ENABLE_CONNECTIVITY_VALIDATION = "dubbo.connectivity.validation";

    String DUBBO_INTERNAL_APPLICATION = "DUBBO_INTERNAL_APPLICATION";

    String RETRY_TIMES_KEY = "retry-times";

    String RETRY_PERIOD_KEY = "retry-period";

    String SYNC_REPORT_KEY = "sync-report";

    String CYCLE_REPORT_KEY = "cycle-report";

    String WORKING_CLASSLOADER_KEY = "WORKING_CLASSLOADER";

    String STAGED_CLASSLOADER_KEY = "STAGED_CLASSLOADER";

    String PROVIDER_ASYNC_KEY = "PROVIDER_ASYNC";

    String REGISTER_IP_KEY = "register.ip";

    String CURRENT_CLUSTER_INVOKER_KEY = "currentClusterInvoker";

    String ENABLE_ROUTER_SNAPSHOT_PRINT_KEY = "ENABLE_ROUTER_SNAPSHOT_PRINT";

    String INJVM_COPY_UTIL_KEY = "injvm-copy-util";

    String INJVM_IGNORE_SAME_MODULE_KEY = "injvm.ignore.same-module";

    String SET_FUTURE_IN_SYNC_MODE = "future.sync.set";

    String CLEAR_FUTURE_AFTER_GET = "future.clear.once";

    String NATIVE_STUB = "nativestub";

    String METADATA = "metadata";

    String IGNORE_LISTEN_SHUTDOWN_HOOK = "dubbo.shutdownHook.listenIgnore";

    String OPTIMIZER_KEY = "optimizer";

    String PREFER_JSON_FRAMEWORK_NAME = "dubbo.json-framework.prefer";

    /**
     * @since 3.1.0
     */
    String MESH_ENABLE = "mesh-enable";

    /**
     * @since 3.1.0
     */
    Integer DEFAULT_MESH_PORT = 80;

    /**
     * @since 3.1.0
     */
    String SVC = ".svc.";

    /**
     * Domain name suffix used inside k8s.
     *
     * @since 3.1.0
     */
    String DEFAULT_CLUSTER_DOMAIN = "cluster.local";

    /**
     * @since 3.1.0
     */
    String UNLOAD_CLUSTER_RELATED = "unloadClusterRelated";

    /**
     * used for thread isolation between services
     */
    String SERVICE_EXECUTOR = "service-executor";

    String EXECUTOR_MANAGEMENT_MODE = "executor-management-mode";

    String EXECUTOR_MANAGEMENT_MODE_DEFAULT = "default";

    String EXECUTOR_MANAGEMENT_MODE_ISOLATION = "isolation";

    /**
     * used in JVMUtil.java ,Control stack print lines, default is 32 lines
     */
    String DUBBO_JSTACK_MAXLINE = "dubbo.jstack-dump.max-line";

    String ENCODE_IN_IO_THREAD_KEY = "encode.in.io";

    boolean DEFAULT_ENCODE_IN_IO_THREAD = false;

    /**
     * @since 3.2.0
     */
    String BYTE_ACCESSOR_KEY = "byte.accessor";

    String PAYLOAD = "payload";

    String DUBBO_METRICS_CONFIGCENTER_ENABLE = "dubbo.metrics.configcenter.enable";

    Integer TRI_EXCEPTION_CODE_NOT_EXISTS = 0;

    String PACKABLE_METHOD_FACTORY_KEY = "serialize.packable.factory";

    String DUBBO_PACKABLE_METHOD_FACTORY = "dubbo.application.parameters." + PACKABLE_METHOD_FACTORY_KEY;

    String DUBBO_MANUAL_REGISTER_KEY = "dubbo.application.manual-register";

    String DUBBO2_COMPACT_ENABLE = "dubbo.compact.enable";

    String ZOOKEEPER_ENSEMBLE_TRACKER_KEY = "zookeeper.ensemble.tracker";

    String DUBBO_VERSIONS_KEY = "META-INF/dubbo-versions";
}
