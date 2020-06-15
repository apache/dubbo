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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MetadataInfo;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public class InstanceAddressURL extends URL {

    private MetadataInfo metadataInfo; // points to metadata of one revision
    private String interfaceName;
    private String group;
    private String version;
    private String serviceKey;

    public InstanceAddressURL(String protocol, String host, int port, String path, String interfaceName, String group, String version, String serviceKey) {
        super(protocol, host, port, path);
        this.interfaceName = interfaceName;
        this.group = group;
        this.version = version;
        this.serviceKey = serviceKey;
    }

    @Override
    public String getServiceInterface() {
        return interfaceName;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String getServiceKey() {
        return serviceKey;
    }

    @Override
    public String getParameter(String key) {
        if (VERSION_KEY.equals(key)) {
            return getVersion();
        } else if (GROUP_KEY.equals(key)) {
            return getGroup();
        } else if (INTERFACE_KEY.equals(key)) {
            return getServiceInterface();
        }

        String value = super.getParameter(key);
        if (StringUtils.isEmpty(value) && metadataInfo != null) {
            value = metadataInfo.getParameter(key, this.getServiceKey());
        }
        return value;
    }

    @Override
    public String getParameter(String key, String defaultValue) {
        if (VERSION_KEY.equals(key)) {
            return getVersion();
        } else if (GROUP_KEY.equals(key)) {
            return getGroup();
        } else if (INTERFACE_KEY.equals(key)) {
            return getServiceInterface();
        }

        String value = getParameter(key);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        return value;
    }

    @Override
    public String getMethodParameter(String method, String key) {
        Map<String, Map<String, String>> instanceMethodParams = super.getMethodParameters();
        Map<String, String> keyMap = instanceMethodParams.get(method);
        String value = null;
        if (keyMap != null) {
            value = keyMap.get(key);
        }

        MetadataInfo.ServiceInfo serviceInfo = metadataInfo.getServiceInfo(getServiceKey());
        value = serviceInfo.getMethodParameter(method, key, value);
        return value;
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> instanceParams = super.getParameters();
        Map<String, String> metadataParams = (metadataInfo == null ? new HashMap<>() : metadataInfo.getParameters(getServiceKey()));
        int i = instanceParams == null ? 0 : instanceParams.size();
        int j = metadataParams == null ? 0 : metadataParams.size();
        Map<String, String> params = new HashMap<>((int) ((i + j) / 0.75) + 1);
        if (instanceParams != null) {
            params.putAll(instanceParams);
        }
        if (metadataParams != null) {
            params.putAll(metadataParams);
        }
        return params;
    }

    public void setMetadata(MetadataInfo metadataInfo) {
        this.metadataInfo = metadataInfo;
    }

}
