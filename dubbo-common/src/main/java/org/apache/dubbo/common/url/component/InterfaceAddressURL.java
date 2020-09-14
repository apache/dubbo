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
package org.apache.dubbo.common.url.component;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;

public class InterfaceAddressURL extends URL {
    public static InterfaceAddressURL valueOf(URLAddress urlAddress, URLParam urlParam, URL consumerUrl) {
        return valueOf(urlAddress, urlParam, consumerUrl, null);
    }

    public static InterfaceAddressURL valueOf(URLAddress urlAddress, URLParam urlParam, URL consumerUrl, URL overrideURL) {
        return new InterfaceAddressURL(
                urlAddress.getProtocol(),
                urlAddress.getUsername(),
                urlAddress.getPassword(),
                urlAddress.getHost(),
                urlAddress.getPort(),
                urlAddress.getPath(),
                urlParam.getParams(),
                consumerUrl,
                overrideURL
        );
    }

    public static InterfaceAddressURL valueOf(String rawURL, URL consumerURL) {
        URL url = valueOf(rawURL, true);
        return new InterfaceAddressURL(
                url.getProtocol(),
                url.getUsername(),
                url.getPassword(),
                url.getHost(),
                url.getPort(),
                url.getPath(),
                url.getParameters(),
                consumerURL,
                null
        );
    }

    private transient URL consumerURL;
    private transient URL overriddenURL;
    private transient Map<String, String> concatenatedPrams;
    private transient long createdStamp;

    public InterfaceAddressURL(
            String protocol,
            String username,
            String password,
            String host,
            int port,
            String path,
            Map<String, String> parameters,
            URL consumerURL,
            URL overrideURL
    ) {
        super(protocol, username, password, host, port, path, parameters);
        // TODO, copied from RegistryDirectory.mergeUrl()
        parameters.put(CHECK_KEY, String.valueOf(false));
        this.consumerURL = consumerURL;
        this.overriddenURL = overrideURL;
        this.createdStamp = System.currentTimeMillis();
    }

    @Override
    public String getApplication() {
        return consumerURL.getApplication();
    }

    @Override
    public String getRemoteApplication() {
        return super.getApplication();
    }

    @Override
    public String getGroup() {
        String group = super.getGroup();
        if (StringUtils.isNotEmpty(group)) {
            return group;
        }
        return consumerURL.getGroup();
    }

    @Override
    public String getVersion() {
        String version = super.getVersion();
        if (StringUtils.isNotEmpty(version)) {
            return version;
        }
        return consumerURL.getVersion();
    }

    @Override
    public String getParameter(String key) {
        String value = null;
        if (overriddenURL != null) {
            value = overriddenURL.getParameter(key);
        }
        if (StringUtils.isEmpty(value) && consumerURL != null) {
            value = consumerURL.getParameter(key);
        }
        if (StringUtils.isEmpty(value)) {
            value = super.getParameter(key);
        }
        return value;
    }

    @Override
    public String getMethodParameter(String method, String key) {
        String value = null;
        if (overriddenURL != null) {
            value = overriddenURL.getMethodParameter(method, key);
        }
        if (StringUtils.isEmpty(value) && consumerURL != null) {
            value = consumerURL.getMethodParameter(method, key);
        }
        if (StringUtils.isEmpty(value)) {
            value = super.getMethodParameter(method, key);
        }
        return value;
    }

    @Override
    public String getConcatenatedParameter(String key) {
        if (concatenatedPrams == null) {
            concatenatedPrams = new HashMap<>();
        }
        String value = concatenatedPrams.get(key);
        if (StringUtils.isNotEmpty(value)) {
            return value;
        }

        // Combine filters and listeners on Provider and Consumer
        String remoteValue = super.getParameter(key);
        String localValue = consumerURL.getParameter(key);
        if (remoteValue != null && remoteValue.length() > 0
                && localValue != null && localValue.length() > 0) {
            concatenatedPrams.put(key, remoteValue + "," + localValue);
        }
        return concatenatedPrams.get(key);
    }

    @Override
    public String getCategory() {
        return super.getParameter(CATEGORY_KEY);
    }

    @Override
    public String getSide() {
        return super.getParameter(SIDE_KEY);
    }

    @Override
    protected <T extends URL> T newURL(
            String protocol,
            String username,
            String password,
            String host,
            int port,
            String path,
            Map<String, String> parameters) {
        return (T) new InterfaceAddressURL(protocol, username, password, host, port, path, parameters, consumerURL, overriddenURL);
    }

    public URL getConsumerURL() {
        return consumerURL;
    }

    public void setConsumerURL(URL consumerURL) {
        this.consumerURL = consumerURL;
    }

    public URL getOverriddenURL() {
        return overriddenURL;
    }

    public void setOverriddenURL(URL overriddenURL) {
        this.overriddenURL = overriddenURL;
    }

    public long getCreatedStamp() {
        return createdStamp;
    }

    public void setCreatedStamp(long createdStamp) {
        this.createdStamp = createdStamp;
    }
}
