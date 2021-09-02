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
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ServiceModel;

import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;

public class DubboServiceAddressURL extends ServiceAddressURL {

    public static DubboServiceAddressURL valueOf(String rawURL, URL consumerURL) {
        return valueOf(rawURL, consumerURL, null);
    }

    public static DubboServiceAddressURL valueOf(String rawURL, URL consumerURL, ServiceConfigURL overriddenURL) {
        URL url = valueOf(rawURL, true);
        return new DubboServiceAddressURL(url.getUrlAddress(), url.getUrlParam(), consumerURL, overriddenURL);
    }

    private ServiceConfigURL overrideURL;

    public DubboServiceAddressURL(URLAddress urlAddress, URLParam urlParam, URL consumerURL, ServiceConfigURL overrideURL) {
        super(urlAddress, urlParam, consumerURL);
        this.overrideURL = overrideURL;
    }

    @Override
    protected <T extends URL> T newURL(URLAddress urlAddress, URLParam urlParam) {
        return (T) new DubboServiceAddressURL(urlAddress, urlParam, this.consumerURL, this.overrideURL);
    }

    @Override
    public String getSide() {
        return consumerURL.getParameter(SIDE_KEY);
    }

    @Override
    public String getParameter(String key) {
        String value = null;
        if (overrideURL != null) {
            value = overrideURL.getParameter(key);
        }
        if (StringUtils.isEmpty(value)) {
            value = super.getParameter(key);
        }
        return value;
    }

    @Override
    public String getMethodParameter(String method, String key) {
        String value = null;
        if (overrideURL != null) {
            value = overrideURL.getMethodParameterStrict(method, key);
        }
        if (StringUtils.isEmpty(value)) {
            value = super.getMethodParameter(method, key);
        }
        return value;
    }

    @Override
    public String getAnyMethodParameter(String key) {
        String value = null;
        if (overrideURL != null) {
            value = overrideURL.getAnyMethodParameter(key);
        }
        if (StringUtils.isEmpty(value)) {
            value = super.getAnyMethodParameter(key);
        }
        return value;
    }

    public ServiceConfigURL getOverrideURL() {
        return overrideURL;
    }

    public void setOverrideURL(ServiceConfigURL overrideURL) {
        this.overrideURL = overrideURL;
    }

    @Override
    public ScopeModel getScopeModel() {
        return consumerURL.getScopeModel();
    }

    @Override
    public ServiceModel getServiceModel() {
        return consumerURL.getServiceModel();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return prime * super.hashCode() + (overrideURL == null ? 0 : overrideURL.hashCode());
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
        if (!(obj instanceof DubboServiceAddressURL)) {
            return false;
        }
        if (overrideURL == null) {
            return super.equals(obj);
        } else {
            DubboServiceAddressURL other = (DubboServiceAddressURL) obj;
            boolean overrideEquals = Objects.equals(overrideURL.getParameters(), other.getOverrideURL().getParameters());
            if (!overrideEquals) {
                return false;
            }

            Map<String, String> params = this.getParameters();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                if (overrideURL.getParameters().containsKey(key)) {
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
