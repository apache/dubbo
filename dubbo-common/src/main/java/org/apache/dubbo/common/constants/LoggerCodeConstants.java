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
 * <p>Constants of Error Codes used in logger.
 *
 * <p>Format: <i>[Category]-[Code]</i>, where:
 * <li>[Category] is the category code which identifies the module.
 * <li>[Code] is the detailed code.
 * <li>Every blanks should be filled with positive number.
 *
 * <br /><br />
 * <p>Hint:
 * <li>Synchronize this file across different branches. (Use merge and cherry-pick.)
 * <li>Double-check the usage in different branches before deleting any of the error code.
 * <li>If applicable, use error code that already appears in this file.
 * <li>If it's required to add an error code, find an error code that's marked by 'Absent', and rename it. (so that no code is wasted)
 * <li>Update the corresponding file in dubbo-website repository.
 */
public interface LoggerCodeConstants {

    // Common module
    String COMMON_THREAD_POOL_EXHAUSTED = "0-1";

    String COMMON_PROPERTY_TYPE_MISMATCH = "0-2";

    String COMMON_CACHE_PATH_INACCESSIBLE = "0-3";

    String COMMON_CACHE_MAX_FILE_SIZE_LIMIT_EXCEED = "0-4";

    String COMMON_CACHE_MAX_ENTRY_COUNT_LIMIT_EXCEED = "0-5";

    String COMMON_THREAD_INTERRUPTED_EXCEPTION = "0-6";

    String COMMON_CLASS_NOT_FOUND = "0-7";

    String COMMON_REFLECTIVE_OPERATION_FAILED = "0-8";

    String COMMON_FAILED_NOTIFY_EVENT = "0-9";

    String COMMON_UNSUPPORTED_INVOKER = "0-10";

    String COMMON_FAILED_STOP_HTTP_SERVER = "0-11";

    String COMMON_UNEXPECTED_EXCEPTION = "0-12";

    String COMMON_METRICS_COLLECTOR_EXCEPTION = "0-13";

    String COMMON_MONITOR_EXCEPTION = "0-14";

    String COMMON_ERROR_LOAD_EXTENSION = "0-15";

    String COMMON_EXECUTORS_NO_FOUND = "0-16";

    String COMMON_UNEXPECTED_EXECUTORS_SHUTDOWN = "0-17";

    String COMMON_ERROR_USE_THREAD_POOL = "0-18";

    String COMMON_ERROR_RUN_THREAD_TASK = "0-19";

    String COMMON_UNEXPECTED_CREATE_DUMP = "0-20";

    String COMMON_ERROR_TOO_MANY_INSTANCES = "0-21";

    String COMMON_IO_EXCEPTION = "0-22";

    String COMMON_JSON_CONVERT_EXCEPTION = "0-23";

    String COMMON_FAILED_OVERRIDE_FIELD = "0-24";

    String COMMON_FAILED_LOAD_MAPPING_CACHE = "0-25";

    String COMMON_METADATA_PROCESSOR = "0-26";

    String COMMON_ISOLATED_EXECUTOR_CONFIGURATION_ERROR = "0-27";

    String VULNERABILITY_WARNING = "0-28";


    // Registry module

    String REGISTRY_ADDRESS_INVALID = "1-1";

    /**
     * Absent. Merged with 0-2.
     */
    String REGISTRY_ABSENCE = "1-2";

    String REGISTRY_FAILED_URL_EVICTING = "1-3";

    String REGISTRY_EMPTY_ADDRESS = "1-4";

    String REGISTRY_NO_PARAMETERS_URL = "1-5";

    String REGISTRY_FAILED_CLEAR_CACHED_URLS = "1-6";

    String REGISTRY_FAILED_NOTIFY_EVENT = "1-7";

    String REGISTRY_FAILED_DESTROY_UNREGISTER_URL = "1-8";

    String REGISTRY_FAILED_READ_WRITE_CACHE_FILE = "1-9";

    String REGISTRY_FAILED_DELETE_LOCKFILE = "1-10";

    String REGISTRY_FAILED_CREATE_INSTANCE = "1-11";

    String REGISTRY_FAILED_FETCH_INSTANCE = "1-12";

    String REGISTRY_EXECUTE_RETRYING_TASK = "1-13";

    String REGISTRY_FAILED_PARSE_DYNAMIC_CONFIG = "1-14";

    String REGISTRY_FAILED_DESTROY_SERVICE = "1-15";

    String REGISTRY_UNSUPPORTED_CATEGORY = "1-16";

    String REGISTRY_FAILED_REFRESH_ADDRESS = "1-17";

    String REGISTRY_MISSING_METADATA_CONFIG_PORT = "1-18";

    String REGISTRY_ERROR_LISTEN_KUBERNETES = "1-19";

    String REGISTRY_UNABLE_MATCH_KUBERNETES = "1-20";

    String REGISTRY_UNABLE_FIND_SERVICE_KUBERNETES = "1-21";

    String REGISTRY_UNABLE_ACCESS_KUBERNETES = "1-22";

    /**
     * Absent. Original '1-23' is changed to '81-3'.
     */
    String REGISTRY_FAILED_DOWNLOAD_FILE = "1-23";

    /**
     * Absent. Original '1-24' is changed to '81-1'.
     */
    String REGISTRY_FAILED_START_ZOOKEEPER = "1-24";

    /**
     * Absent. Original '1-25' is changed to '81-2'.
     */
    String REGISTRY_FAILED_STOP_ZOOKEEPER = "1-25";

    String REGISTRY_FAILED_GENERATE_CERT_ISTIO = "1-26";

    String REGISTRY_FAILED_GENERATE_KEY_ISTIO = "1-27";

    String REGISTRY_RECEIVE_ERROR_MSG_ISTIO = "1-28";

    String REGISTRY_ERROR_READ_FILE_ISTIO = "1-29";

    String REGISTRY_ERROR_REQUEST_XDS = "1-30";

    String REGISTRY_ERROR_RESPONSE_XDS = "1-31";

    String REGISTRY_ERROR_CREATE_CHANNEL_XDS = "1-32";

    String REGISTRY_ERROR_INITIALIZE_XDS = "1-33";

    String REGISTRY_ERROR_PARSING_XDS = "1-34";

    String REGISTRY_ZOOKEEPER_EXCEPTION = "1-35";

    /**
     * Absent. Merged with 99-0.
     */
    String REGISTRY_UNEXPECTED_EXCEPTION = "1-36";

    String REGISTRY_NACOS_EXCEPTION = "1-37";

    String REGISTRY_SOCKET_EXCEPTION = "1-38";

    String REGISTRY_FAILED_LOAD_METADATA = "1-39";

    String REGISTRY_ROUTER_WAIT_LONG = "1-40";

    String REGISTRY_ISTIO_EXCEPTION = "1-41";

    String REGISTRY_NACOS_SUB_LEGACY = "1-42";

    // Cluster module 2-x
    String CLUSTER_FAILED_SITE_SELECTION = "2-1";

    String CLUSTER_NO_VALID_PROVIDER = "2-2";

    String CLUSTER_FAILED_STOP = "2-3";

    String CLUSTER_FAILED_LOAD_MERGER = "2-4";

    String CLUSTER_FAILED_RESELECT_INVOKERS = "2-5";

    String CLUSTER_CONDITIONAL_ROUTE_LIST_EMPTY = "2-6";

    String CLUSTER_FAILED_EXEC_CONDITION_ROUTER = "2-7";

    String CLUSTER_ERROR_RESPONSE = "2-8";

    String CLUSTER_TIMER_RETRY_FAILED = "2-9";

    String CLUSTER_FAILED_INVOKE_SERVICE = "2-10";

    String CLUSTER_TAG_ROUTE_INVALID = "2-11";

    String CLUSTER_TAG_ROUTE_EMPTY = "2-12";

    String CLUSTER_FAILED_RECEIVE_RULE = "2-13";

    String CLUSTER_SCRIPT_EXCEPTION = "2-14";

    String CLUSTER_FAILED_RULE_PARSING = "2-15";

    String CLUSTER_FAILED_MULTIPLE_RETRIES = "2-16";

    String CLUSTER_FAILED_MOCK_REQUEST = "2-17";

    String CLUSTER_NO_RULE_LISTENER = "2-18";

    String CLUSTER_EXECUTE_FILTER_EXCEPTION = "2-19";

    String CLUSTER_FAILED_GROUP_MERGE = "2-20";

    // Proxy module. 3-1
    String PROXY_FAILED_CONVERT_URL = "3-1";

    String PROXY_FAILED_EXPORT_SERVICE = "3-2";

    /**
     * Absent. Merged with 3-8.
     */
    String PROXY_33 = "3-3";

    String PROXY_TIMEOUT_REQUEST = "3-4";

    String PROXY_ERROR_ASYNC_RESPONSE = "3-5";

    String PROXY_UNSUPPORTED_INVOKER = "3-6";

    String PROXY_TIMEOUT_RESPONSE = "3-7";

    String PROXY_FAILED = "3-8";

    // Protocol module.
    String PROTOCOL_UNSUPPORTED = "4-1";

    String PROTOCOL_FAILED_INIT_SERIALIZATION_OPTIMIZER = "4-2";

    String PROTOCOL_FAILED_REFER_INVOKER = "4-3";

    String PROTOCOL_UNSAFE_SERIALIZATION = "4-4";

    String PROTOCOL_FAILED_CLOSE_STREAM = "4-5";

    String PROTOCOL_ERROR_DESERIALIZE = "4-6";

    String PROTOCOL_ERROR_CLOSE_CLIENT = "4-7";

    String PROTOCOL_ERROR_CLOSE_SERVER = "4-8";

    String PROTOCOL_FAILED_PARSE = "4-9";

    String PROTOCOL_FAILED_SERIALIZE_TRIPLE = "4-10";

    String PROTOCOL_FAILED_REQUEST = "4-11";

    String PROTOCOL_FAILED_CREATE_STREAM_TRIPLE = "4-12";

    String PROTOCOL_TIMEOUT_SERVER = "4-13";

    String PROTOCOL_FAILED_RESPONSE = "4-14";

    String PROTOCOL_STREAM_LISTENER = "4-15";

    String PROTOCOL_CLOSED_SERVER = "4-16";

    String PROTOCOL_FAILED_DESTROY_INVOKER = "4-17";

    String PROTOCOL_FAILED_LOAD_MODEL = "4-18";

    String PROTOCOL_INCORRECT_PARAMETER_VALUES = "4-19";

    String PROTOCOL_FAILED_DECODE = "4-20";

    String PROTOCOL_UNTRUSTED_SERIALIZE_CLASS = "4-21";

    // Config module
    String CONFIG_FAILED_CONNECT_REGISTRY = "5-1";

    String CONFIG_FAILED_SHUTDOWN_HOOK = "5-2";

    String CONFIG_FAILED_DESTROY_INVOKER = "5-3";

    String CONFIG_NO_METHOD_FOUND = "5-4";

    String CONFIG_FAILED_LOAD_ENV_VARIABLE = "5-5";

    String CONFIG_PROPERTY_CONFLICT = "5-6";

    String CONFIG_UNEXPORT_ERROR = "5-7";

    String CONFIG_USE_RANDOM_PORT = "5-8";

    String CONFIG_FAILED_EXPORT_SERVICE = "5-9";

    String CONFIG_SERVER_DISCONNECTED = "5-10";

    String CONFIG_REGISTER_INSTANCE_ERROR = "5-11";

    String CONFIG_REFRESH_INSTANCE_ERROR = "5-12";

    String CONFIG_UNABLE_DESTROY_MODEL = "5-13";

    String CONFIG_FAILED_START_MODEL = "5-14";

    String CONFIG_FAILED_REFERENCE_MODEL = "5-15";

    String CONFIG_FAILED_FIND_PROTOCOL = "5-16";

    String CONFIG_PARAMETER_FORMAT_ERROR = "5-17";

    String CONFIG_FAILED_NOTIFY_EVENT = "5-18";

    /**
     * Absent. Changed to 81-4.
     */
    String CONFIG_ZOOKEEPER_SERVER_ERROR = "5-19";

    String CONFIG_STOP_DUBBO_ERROR = "5-20";

    String CONFIG_FAILED_EXECUTE_DESTROY = "5-21";

    String CONFIG_FAILED_INIT_CONFIG_CENTER = "5-22";

    String CONFIG_FAILED_WAIT_EXPORT_REFER = "5-23";

    String CONFIG_FAILED_REFER_SERVICE = "5-24";

    String CONFIG_UNDEFINED_PROTOCOL = "5-25";

    String CONFIG_METADATA_SERVICE_EXPORTED = "5-26";

    String CONFIG_API_WRONG_USE = "5-27";

    String CONFIG_NO_ANNOTATIONS_FOUND = "5-28";

    String CONFIG_NO_BEANS_SCANNED = "5-29";

    String CONFIG_DUPLICATED_BEAN_DEFINITION = "5-30";

    String CONFIG_WARN_STATUS_CHECKER = "5-31";

    String CONFIG_FAILED_CLOSE_CONNECT_APOLLO = "5-32";

    String CONFIG_NOT_EFFECT_EMPTY_RULE_APOLLO = "5-33";

    String CONFIG_ERROR_NACOS = "5-34";

    String CONFIG_START_DUBBO_ERROR = "5-35";

    String CONFIG_FILTER_VALIDATION_EXCEPTION = "5-36";

    String CONFIG_ERROR_PROCESS_LISTENER = "5-37";

    String CONFIG_UNDEFINED_ARGUMENT = "5-38";

    String CONFIG_DUBBO_BEAN_INITIALIZER = "5-39";

    String CONFIG_DUBBO_BEAN_NOT_FOUND = "5-40";

    String CONFIG_SSL_PATH_LOAD_FAILED = "5-41";

    String CONFIG_SSL_CERT_GENERATE_FAILED = "5-42";

    String CONFIG_SSL_CONNECT_INSECURE = "5-43";

    // Transport module
    String TRANSPORT_FAILED_CONNECT_PROVIDER = "6-1";

    String TRANSPORT_CLIENT_CONNECT_TIMEOUT = "6-2";

    String TRANSPORT_FAILED_CLOSE = "6-3";

    /**
     * Absent. Merged to 99-0.
     */
    String TRANSPORT_UNEXPECTED_EXCEPTION = "6-4";

    String TRANSPORT_FAILED_DISCONNECT_PROVIDER = "6-5";

    String TRANSPORT_UNSUPPORTED_MESSAGE = "6-6";

    String TRANSPORT_CONNECTION_LIMIT_EXCEED = "6-7";

    String TRANSPORT_FAILED_DECODE = "6-8";

    String TRANSPORT_FAILED_SERIALIZATION = "6-9";

    String TRANSPORT_EXCEED_PAYLOAD_LIMIT = "6-10";

    String TRANSPORT_UNSUPPORTED_CHARSET = "6-11";

    String TRANSPORT_FAILED_DESTROY_ZOOKEEPER = "6-12";

    String TRANSPORT_FAILED_CLOSE_STREAM = "6-13";

    String TRANSPORT_FAILED_RESPONSE = "6-14";

    String TRANSPORT_SKIP_UNUSED_STREAM = "6-15";

    String TRANSPORT_FAILED_RECONNECT = "6-16";

    // qos plugin
    String QOS_PROFILER_DISABLED = "7-1";

    String QOS_PROFILER_ENABLED = "7-2";

    String QOS_PROFILER_WARN_PERCENT = "7-3";

    String QOS_FAILED_START_SERVER = "7-4";

    String QOS_COMMAND_NOT_FOUND = "7-5";

    String QOS_UNEXPECTED_EXCEPTION = "7-6";

    String QOS_PERMISSION_DENY_EXCEPTION = "7-7";

    // Testing module (8[X], where [X] is number of the module to be tested.)
    String TESTING_REGISTRY_FAILED_TO_START_ZOOKEEPER = "81-1";

    String TESTING_REGISTRY_FAILED_TO_STOP_ZOOKEEPER = "81-2";

    String TESTING_REGISTRY_FAILED_TO_DOWNLOAD_ZK_FILE = "81-3";

    String TESTING_INIT_ZOOKEEPER_SERVER_ERROR = "81-4";

    // Internal unknown error.

    /**
     * Unknown internal error. (99-0)
     */
    String INTERNAL_ERROR = "99-0";

    String INTERNAL_INTERRUPTED = "99-1";
}
