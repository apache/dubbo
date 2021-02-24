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

    String REGISTRY_KEY = "registry";

    String REGISTRY_CLUSTER_KEY = "REGISTRY_CLUSTER";

    String REGISTRY_CLUSTER = "REGISTRY_CLUSTER";

    String REGISTRY_CLUSTER_TYPE_KEY = "registry-cluster-type";

    String REGISTRY_PROTOCOL = "registry";

    String DYNAMIC_KEY = "dynamic";

    String CATEGORY_KEY = "category";

    String PROVIDERS_CATEGORY = "providers";

    String CONSUMERS_CATEGORY = "consumers";

    String ROUTERS_CATEGORY = "routers";

    String DYNAMIC_ROUTERS_CATEGORY = "dynamicrouters";

    String DEFAULT_CATEGORY = PROVIDERS_CATEGORY;

    String CONFIGURATORS_CATEGORY = "configurators";

    String DYNAMIC_CONFIGURATORS_CATEGORY = "dynamicconfigurators";

    String APP_DYNAMIC_CONFIGURATORS_CATEGORY = "appdynamicconfigurators";

    String ROUTERS_SUFFIX = ".routers";

    String EMPTY_PROTOCOL = "empty";

    String ROUTE_PROTOCOL = "route";

    String OVERRIDE_PROTOCOL = "override";

    String COMPATIBLE_CONFIG_KEY = "compatible_config";

    String REGISTRY_PUBLISH_INTERFACE_KEY = "publish-interface";

    String DUBBO_PUBLISH_INTERFACE_DEFAULT_KEY = "dubbo.application.publish-interface";
    /**
     * The parameter key of Dubbo Registry type
     *
     * @since 2.7.5
     */
    String REGISTRY_TYPE_KEY = "registry-type";

    /**
     * The parameter value of Service-Oriented Registry type
     *
     * @since 2.7.5
     */
    String SERVICE_REGISTRY_TYPE = "service";

    /**
     * The protocol for Service Discovery
     *
     * @since 2.7.5
     */
    String SERVICE_REGISTRY_PROTOCOL = "service-discovery-registry";

    /**
     * The parameter key of the subscribed service names for Service-Oriented Registry
     * <p>
     * If there is a multiple-values, the  "comma" is the separator.
     *
     * @since 2.7.5
     */
    String SUBSCRIBED_SERVICE_NAMES_KEY = "subscribed-services";

    String PROVIDED_BY = "provided-by";

    /**
     * The request size of service instances
     *
     * @since 2.7.5
     */
    String INSTANCES_REQUEST_SIZE_KEY = "instances-request-size";

    /**
     * The default request size of service instances
     */
    int DEFAULT_INSTANCES_REQUEST_SIZE = 100;

    String ACCEPTS_KEY = "accepts";

    String REGISTRY_ZONE = "registry_zone";
    String REGISTRY_ZONE_FORCE = "registry_zone_force";
    String ZONE_KEY = "zone";

    String REGISTRY_SERVICE_REFERENCE_PATH = "org.apache.dubbo.registry.RegistryService";

    String INIT = "INIT";

    boolean MIGRATION_MULTI_REGSITRY = false;

    String LOADBALANCE_AMONG_REGISTRIES = "random";
}
