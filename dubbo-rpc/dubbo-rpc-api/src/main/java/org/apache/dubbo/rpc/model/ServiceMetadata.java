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
package org.apache.dubbo.rpc.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * data related to service level such as name, version, classloader of business service,
 * security info, etc. Also with a AttributeMap for extension.
 */
public class ServiceMetadata {

    private final String serviceKey;
    private final String serviceInterfaceName;
    private final String defaultGroup;
    private final String version;
    private final Class<?> serviceType;

    private volatile String group;

    /* will be transferred to remote side */
    private final Map<String, Object> attachments = new ConcurrentHashMap<String, Object>();
    /* used locally*/
    private final Map<String, Object> attributeMap = new ConcurrentHashMap<String, Object>();

    public ServiceMetadata(String serviceInterfaceName, String group, String version, Class<?> serviceType) {
        this.serviceInterfaceName = serviceInterfaceName;
        this.defaultGroup = group;
        this.group = group;
        this.version = version;
        this.serviceKey = serviceInterfaceName + ":" + version;
        this.serviceType = serviceType;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }

    public Map<String, Object> getAttributeMap() {
        return attributeMap;
    }

    public void addAttribute(String key, Object value) {
        this.attributeMap.put(key, value);
    }

    public void addAttachment(String key, Object value) {
        this.attributeMap.put(key, value);
    }

    public Class<?> getServiceType() {
        return serviceType;
    }

    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    public String getDefaultGroup() {
        return defaultGroup;
    }

    public String getVersion() {
        return version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
