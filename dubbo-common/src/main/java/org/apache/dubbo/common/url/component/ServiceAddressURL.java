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
import java.util.Objects;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;

/**
 * This class derived from URL, so the size of one single URL is not reduced.
 * but it can avoid the map copy from consumer url to provider url by holding the consumer url reference directly.
 */
public class ServiceAddressURL extends URL {
    public static ServiceAddressURL valueOf(URLAddress urlAddress, URLParam urlParam, URL consumerUrl) {
        return valueOf(urlAddress, urlParam, consumerUrl, null);
    }

    public static ServiceAddressURL valueOf(URLAddress urlAddress, URLParam urlParam, URL consumerURL, URL overrideURL) {
        return new ServiceAddressURL(urlAddress, urlParam, consumerURL, overrideURL);
    }

    public static ServiceAddressURL valueOf(String rawURL, URL consumerURL) {
        return valueOf(rawURL, consumerURL, null);
    }

    public static ServiceAddressURL valueOf(String rawURL, URL consumerURL, URL overriddenURL) {
        URL url = valueOf(rawURL, true);
        return new ServiceAddressURL(url.getUrlAddress(), url.getUrlParam(), consumerURL, overriddenURL);
    }

    private final transient URL consumerURL;
    private final transient URL overriddenURL;

    //cache
    private transient Map<String, String> concatenatedPrams;
//    private transient Map<String, String> allParameters;
    private transient long createdStamp;

    public ServiceAddressURL(
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
        this.consumerURL = consumerURL;
        this.overriddenURL = overrideURL;
        this.createdStamp = System.currentTimeMillis();
    }

    public ServiceAddressURL(URLAddress urlAddress, URLParam urlParam, URL consumerURL, URL overrideURL){
        super(urlAddress, urlParam);
        this.consumerURL = consumerURL;
        this.overriddenURL = overrideURL;
        this.createdStamp = System.currentTimeMillis();
    }

    @Override
    public String getPath() {
        return consumerURL.getPath();
    }

    @Override
    public String getServiceInterface() {
        return consumerURL.getServiceInterface();
    }

    @Override
    public String getApplication() {
        return consumerURL.getApplication();
    }

    @Override
    public String getRemoteApplication() {
        return super.getParameter(APPLICATION_KEY);
    }

    @Override
    public String getGroup() {
        String group = super.getParameter(GROUP_KEY);
        if (StringUtils.isNotEmpty(group)) {
            return group;
        }
        return consumerURL.getGroup();
    }

    @Override
    public String getVersion() {
        String version = super.getParameter(VERSION_KEY);
        if (StringUtils.isNotEmpty(version)) {
            return version;
        }
        return consumerURL.getVersion();
    }

    /**
     * FIXME, Avoid calling this method on the main line.
     */
//    @Override
//    public Map<String, String> getParameters() {
//        Map<String, String> allParameters = new HashMap<>((int)(super.getParameters().size()/.75 + 1));
//        allParameters.putAll(super.getParameters());
//        if (consumerURL != null) {
//            allParameters.putAll(consumerURL.getParameters());
//        }
//        if (overriddenURL != null) {
//            allParameters.putAll(overriddenURL.getParameters());
//        }
//        allParameters.remove(CATEGORY_KEY);
//        return Collections.unmodifiableMap(allParameters);
//    }

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
            value = overriddenURL.getMethodParameterStrict(method, key);
        }
        if (StringUtils.isEmpty(value) && consumerURL != null) {
            value = consumerURL.getMethodParameterStrict(method, key);
        }
        if (StringUtils.isEmpty(value)) {
            value = super.getMethodParameterStrict(method, key);
        }
        if (StringUtils.isEmpty(value)) {
            value = getParameter(key);
        }
        return value;
    }

    @Override
    public String getAnyMethodParameter(String key) {
        String value = null;
        if (overriddenURL != null) {
            value = overriddenURL.getAnyMethodParameter(key);
        }
        if (StringUtils.isEmpty(value) && consumerURL != null) {
            value = consumerURL.getAnyMethodParameter(key);
        }
        if (StringUtils.isEmpty(value)) {
            value = super.getAnyMethodParameter(key);
        }
        return value;
    }

    @Override
    public String getConcatenatedParameter(String key) {
        if (concatenatedPrams == null) {
            concatenatedPrams = new HashMap<>(1);
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
            value = remoteValue + "," + localValue;
            concatenatedPrams.put(key, value);
            return value;
        }
        if (localValue != null && localValue.length() > 0) {
            value = localValue;
        } else if (remoteValue != null && remoteValue.length() > 0) {
            value = remoteValue;
        }
        concatenatedPrams.put(key, value);
        return value;
    }

    @Override
    public String getCategory() {
        return PROVIDERS_CATEGORY;
    }

    @Override
    public String getSide() {
        return CONSUMER_SIDE;
    }

    protected <T extends URL> T newURL(URLAddress urlAddress, URLParam urlParam) {
        return (T) new ServiceAddressURL(urlAddress, urlParam, this.consumerURL, this.overriddenURL);
    }

    public URL getConsumerURL() {
        return consumerURL;
    }

    public URL getOverriddenURL() {
        return overriddenURL;
    }

    public long getCreatedStamp() {
        return createdStamp;
    }

    public void setCreatedStamp(long createdStamp) {
        this.createdStamp = createdStamp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return prime * super.hashCode() + (overriddenURL == null ? 0 : overriddenURL.hashCode());
    }

    /**
     * ignore consumer url compare.
     * It's only meaningful for comparing two AddressURLs related to the same consumerURL.
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ServiceAddressURL)) {
            return false;
        }
        if (overriddenURL == null) {
            return super.equals(obj);
        } else {
            ServiceAddressURL other = (ServiceAddressURL) obj;
            boolean overrideEquals = Objects.equals(overriddenURL.getParameters(), other.getOverriddenURL().getParameters());
            if (!overrideEquals) {
                return false;
            }

            Map<String, String> params = this.getParameters();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                if (overriddenURL.getParameters().containsKey(key)) {
                    continue;
                }
                if (!entry.getValue().equals(other.getUrlParam().getParameter(key))) {
                    return false;
                }
            }
        }
        return true;
    }
}
