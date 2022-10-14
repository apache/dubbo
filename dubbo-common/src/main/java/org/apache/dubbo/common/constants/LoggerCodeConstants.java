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
 * constants for logger
 */
public interface LoggerCodeConstants {

    // common module 0-1 ~ 0-4
    String COMMON_THREAD_POOL_EXHAUSTED = "0-1";

    String COMMON_PROPERTY_MISSPELLING = "0-2";

    String COMMON_CACHE_PATH_INACCESSIBLE = "0-3";

    String COMMON_CACHE_MAX_FILE_SIZE_LIMIT_EXCEED = "0-4";


    String COMMON_CACHE_MAX_ENTRY_COUNT_LIMIT_EXCEED = "0-5";

    // registry module
    String REGISTRY_ADDRESS_INVALID = "1-1";

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

    String REGISTRY_ROUTER_WAIT_LONG = "1-19";

    // cluster module 2-1 ~ 2-18
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

    // proxy module 3-1
    String PROXY_FAILED_CONVERT_URL = "3-1";

    // protocol module 4-1 ~ 4-3
    String PROTOCOL_UNSUPPORTED = "4-1";

    String PROTOCOL_FAILED_INIT_SERIALIZATION_OPTIMIZER = "4-2";

    String PROTOCOL_FAILED_REFER_INVOKER = "4-3";

    // config module 5-1 ~ 5-20
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

    String CONFIG_ZOOKEEPER_SERVER_ERROR = "5-19";

    String CONFIG_STOP_DUBBO_ERROR = "5-20";

    // transport module 6-1 ~ 6-2
    String TRANSPORT_FAILED_CONNECT_PROVIDER = "6-1";

    String TRANSPORT_CLIENT_CONNECT_TIMEOUT = "6-2";

    String INTERNAL_SERVICE_CONFIG_ERROR = "6-3";

    String INTERNAL_ERROR = "99-0";

    String INTERNAL_INTERRUPTED = "99-1";
}
