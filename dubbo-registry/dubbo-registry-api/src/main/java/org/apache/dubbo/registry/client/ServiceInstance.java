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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.beans.Transient;
import java.io.Serializable;
import java.util.Map;
import java.util.SortedMap;

/**
 * The model class of an instance of a service, which is used for service registration and discovery.
 * <p>
 *
 * @since 2.7.5
 */
public interface ServiceInstance extends Serializable {

    /**
     * The name of service that current instance belongs to.
     *
     * @return non-null
     */
    String getServiceName();

    /**
     * The hostname of the registered service instance.
     *
     * @return non-null
     */
    String getHost();

    /**
     * The port of the registered service instance.
     *
     * @return the positive integer if present
     */
    int getPort();

    String getAddress();

    /**
     * The enabled status of the registered service instance.
     *
     * @return if <code>true</code>, indicates current instance is enabled, or disable, the client should remove this one.
     * The default value is <code>true</code>
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * The registered service instance is health or not.
     *
     * @return if <code>true</code>, indicates current instance is healthy, or unhealthy, the client may ignore this one.
     * The default value is <code>true</code>
     */
    default boolean isHealthy() {
        return true;
    }

    /**
     * The key / value pair metadata associated with the service instance.
     *
     * @return non-null, mutable and unsorted {@link Map}
     */
    Map<String, String> getMetadata();

    SortedMap<String, String> getSortedMetadata();

    String getRegistryCluster();

    void setRegistryCluster(String registryCluster);

    Map<String, String> getExtendParams();

    String getExtendParam(String key);

    String putExtendParam(String key, String value);

    String putExtendParamIfAbsent(String key, String value);

    String removeExtendParam(String key);

    Map<String, String> getAllParams();

    void setApplicationModel(ApplicationModel applicationModel);

    ApplicationModel getApplicationModel();

    @Transient
    default ApplicationModel getOrDefaultApplicationModel() {
        return ScopeModelUtil.getApplicationModel(getApplicationModel());
    }

    /**
     * Get the value of metadata by the specified name
     *
     * @param name the specified name
     * @return the value of metadata if found, or <code>null</code>
     * @since 2.7.8
     */
    default String getMetadata(String name) {
        return getMetadata(name, null);
    }

    /**
     * Get the value of metadata by the specified name
     *
     * @param name the specified name
     * @return the value of metadata if found, or <code>defaultValue</code>
     * @since 2.7.8
     */
    default String getMetadata(String name, String defaultValue) {
        return getMetadata().getOrDefault(name, defaultValue);
    }

    MetadataInfo getServiceMetadata();

    void setServiceMetadata(MetadataInfo serviceMetadata);

    InstanceAddressURL toURL();

}
