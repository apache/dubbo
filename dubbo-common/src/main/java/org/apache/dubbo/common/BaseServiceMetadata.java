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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ServiceModel;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_VERSION;

/**
 * 2019-10-10
 */
public class BaseServiceMetadata {
    public static final char COLON_SEPARATOR = ':';
    private static final int DEFAULT_PORT = 20880; // Default Dubbo port

    protected String serviceKey;
    protected String serviceInterfaceName;
    protected String version;
    protected volatile String group;
    private ServiceModel serviceModel;

    public static String buildServiceKey(String path, String group, String version, int port) {
        int length = path == null ? 0 : path.length();
        length += group == null ? 0 : group.length();
        length += version == null ? 0 : version.length();
        length += 10; // Additional space for port and separators
        StringBuilder buf = new StringBuilder(length);
        if (StringUtils.isNotEmpty(group)) {
            buf.append(group).append('/');
        }
        buf.append(path);
        if (StringUtils.isNotEmpty(version)) {
            buf.append(':').append(version);
        }
        buf.append(':').append(port); // Add port to the service key
        return buf.toString();
    }

    public static int portFromServiceKey(String serviceKey) {
        int lastColonIndex = serviceKey.lastIndexOf(':');
        if (lastColonIndex == -1) {
            throw new IllegalArgumentException("Invalid service key format: " + serviceKey);
        }
        return Integer.parseInt(serviceKey.substring(lastColonIndex + 1));
    }

    public static String versionFromServiceKey(String serviceKey) {
        int firstColonIndex = serviceKey.indexOf(':');
        int lastColonIndex = serviceKey.lastIndexOf(':');
        if (firstColonIndex == -1 || lastColonIndex == firstColonIndex) {
            return DEFAULT_VERSION;
        }
        return serviceKey.substring(firstColonIndex + 1, lastColonIndex);
    }

    public static String groupFromServiceKey(String serviceKey) {
        int groupIndex = serviceKey.indexOf('/');
        if (groupIndex == -1) {
            return null;
        }
        return serviceKey.substring(0, groupIndex);
    }

    public static String interfaceFromServiceKey(String serviceKey) {
        int groupIndex = serviceKey.indexOf('/');
        int versionIndex = serviceKey.indexOf(':');
        groupIndex = (groupIndex == -1) ? 0 : groupIndex + 1;
        versionIndex = (versionIndex == -1) ? serviceKey.length() : versionIndex;
        return serviceKey.substring(groupIndex, versionIndex);
    }

    public String getDisplayServiceKey() {
        StringBuilder serviceNameBuilder = new StringBuilder();
        serviceNameBuilder.append(serviceInterfaceName);
        serviceNameBuilder.append(COLON_SEPARATOR).append(version);
        return serviceNameBuilder.toString();
    }

    public static BaseServiceMetadata revertDisplayServiceKey(String displayKey) {
        String[] eles = StringUtils.split(displayKey, COLON_SEPARATOR);
        if (eles == null || eles.length < 1 || eles.length > 2) {
            return new BaseServiceMetadata();
        }
        BaseServiceMetadata serviceDescriptor = new BaseServiceMetadata();
        serviceDescriptor.setServiceInterfaceName(eles[0]);
        if (eles.length == 2) {
            serviceDescriptor.setVersion(eles[1]);
        }
        return serviceDescriptor;
    }

    public static String keyWithoutGroup(String interfaceName, String version) {
        if (StringUtils.isEmpty(version)) {
            return interfaceName + ":" + DEFAULT_VERSION;
        }
        return interfaceName + ":" + version;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void generateServiceKey() {
        this.serviceKey = buildServiceKey(serviceInterfaceName, group, version, DEFAULT_PORT);
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    public void setServiceInterfaceName(String serviceInterfaceName) {
        this.serviceInterfaceName = serviceInterfaceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ServiceModel getServiceModel() {
        return serviceModel;
    }

    public void setServiceModel(ServiceModel serviceModel) {
        this.serviceModel = serviceModel;
    }
}
