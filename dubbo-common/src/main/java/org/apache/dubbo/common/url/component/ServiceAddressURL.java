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

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;

public abstract class ServiceAddressURL extends URL {
    protected final transient URL consumerURL;

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
            URL consumerURL
    ) {
        super(protocol, username, password, host, port, path, parameters);
        this.consumerURL = consumerURL;
        this.createdStamp = System.currentTimeMillis();
    }

    public ServiceAddressURL(URLAddress urlAddress, URLParam urlParam, URL consumerURL){
        super(urlAddress, urlParam);
        this.consumerURL = consumerURL;
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
        if (consumerURL != null) {
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
        if (consumerURL != null) {
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
        if (consumerURL != null) {
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

    public URL getConsumerURL() {
        return consumerURL;
    }

    public long getCreatedStamp() {
        return createdStamp;
    }

    public void setCreatedStamp(long createdStamp) {
        this.createdStamp = createdStamp;
    }

    @Override
    public int hashCode() {
        return super.hashCode() ;
    }

    /**
     * ignore consumer url compare.
     * It's only meaningful for comparing two address urls related to the same consumerURL.
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
        return super.equals(obj);
    }
}
