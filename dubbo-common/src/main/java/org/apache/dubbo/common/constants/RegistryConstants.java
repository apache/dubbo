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

public interface RegistryConstants {
    String REGISTER_KEY = "register";

    String SUBSCRIBE_KEY = "subscribe";

    String REGISTRY_KEY = "registry";

    String DEFAULT_REGISTRY = "dubbo";

    String REGISTRY_PROTOCOL = "registry";

    String DYNAMIC_KEY = "dynamic";

    String REGISTER = "register";

    String UNREGISTER = "unregister";

    String SUBSCRIBE = "subscribe";

    String UNSUBSCRIBE = "unsubscribe";

    String CATEGORY_KEY = "category";

    String PROVIDERS_CATEGORY = "providers";

    String CONSUMERS_CATEGORY = "consumers";

    String ROUTERS_CATEGORY = "routers";

    String DYNAMIC_ROUTERS_CATEGORY = "dynamicrouters";

    String DEFAULT_CATEGORY = PROVIDERS_CATEGORY;

    String CONFIGURATORS_CATEGORY = "configurators";

    String DYNAMIC_CONFIGURATORS_CATEGORY = "dynamicconfigurators";

    String APP_DYNAMIC_CONFIGURATORS_CATEGORY = "appdynamicconfigurators";

    String CONFIGURATORS_SUFFIX = ".configurators";

    String ROUTERS_SUFFIX = ".routers";

    String TRACE_PROTOCOL = "trace";

    String EMPTY_PROTOCOL = "empty";

    String ADMIN_PROTOCOL = "admin";

    String PROVIDER_PROTOCOL = "provider";

    String CONSUMER_PROTOCOL = "consumer";

    String ROUTE_PROTOCOL = "route";

    String SCRIPT_PROTOCOL = "script";

    String CONDITION_PROTOCOL = "condition";

    /**
     * simple the registry for provider.
     *
     * @since 2.7.0
     */
    String SIMPLIFIED_KEY = "simplified";

    /**
     * After simplify the registry, should add some paramter individually for provider.
     *
     * @since 2.7.0
     */
    String EXTRA_KEYS_KEY = "extra-keys";

    String OVERRIDE_PROTOCOL = "override";

    String COMPATIBLE_CONFIG_KEY = "compatible_config";

    /**
     * To decide whether register center saves file synchronously, the default value is asynchronously
     */
    String REGISTRY_FILESAVE_SYNC_KEY = "save.file";

    /**
     * Period of registry center's retry interval
     */
    String REGISTRY_RETRY_PERIOD_KEY = "retry.period";

    /**
     * Most retry times
     */
    String REGISTRY_RETRY_TIMES_KEY = "retry.times";

    /**
     * Default value for the period of retry interval in milliseconds: 5000
     */
    int DEFAULT_REGISTRY_RETRY_PERIOD = 5 * 1000;

    /**
     * Default value for the times of retry: 3
     */
    int DEFAULT_REGISTRY_RETRY_TIMES = 3;

    /**
     * Reconnection period in milliseconds for register center
     */
    String REGISTRY_RECONNECT_PERIOD_KEY = "reconnect.period";

    int DEFAULT_REGISTRY_RECONNECT_PERIOD = 3 * 1000;

    String SESSION_TIMEOUT_KEY = "session";

    int DEFAULT_SESSION_TIMEOUT = 60 * 1000;
    /**
     * key for router type, for e.g., "script"/"file",  corresponding to ScriptRouterFactory.NAME, FileRouterFactory.NAME
     */
    String ROUTER_KEY = "router";

    /**
     * The key name for export URL in register center
     */
    String EXPORT_KEY = "export";
    /**
     * The key name for reference URL in register center
     */
    String REFER_KEY = "refer";
}
