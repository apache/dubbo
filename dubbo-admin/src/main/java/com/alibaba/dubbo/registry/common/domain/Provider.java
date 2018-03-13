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
package com.alibaba.dubbo.registry.common.domain;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.common.registry.ConvertUtil;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Provider
 *
 */
public class Provider extends Entity {

    private static final long serialVersionUID = 5981342400350878171L;

    private String service;/* The name of the service provided by the provider */

    private String url; /* Provider's address for service */

    private String parameters; /* Provider provides service parameters */

    private String address; /* Provider address */

    private String registry;/* The provider's registry address */

    private boolean dynamic;          /* provider was registered dynamically */

    private boolean enabled;          /* provider enabled or not */

    private int weight;          /* provider weight */

    private String application; /* application name */

    private String username;      /* operator */

    private Date expired;   /* time to expire */

    private long alived;    /* time to live in milliseconds */

    private Override override;

    private List<Override> overrides;

    public Provider() {
    }

    public Provider(Long id) {
        super(id);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRegistry() {
        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    public Date getExpired() {
        return expired;
    }


    public void setExpired(Date expired) {
        this.expired = expired;
    }

    public long getAlived() {
        return alived;
    }

    public void setAlived(long aliveSeconds) {
        this.alived = aliveSeconds;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Override getOverride() {
        return override;
    }

    public void setOverride(Override override) {
        this.override = override;
    }

    public List<Override> getOverrides() {
        return overrides;
    }

    public void setOverrides(List<Override> overrides) {
        this.overrides = overrides;
    }

    public URL toUrl() {
        Map<String, String> serviceName2Map = ConvertUtil.serviceName2Map(getService());
        /*if(!serviceName2Map.containsKey(Constants.INTERFACE_KEY)) {
            throw new IllegalArgumentException("No interface info");
        }
        if(!serviceName2Map.containsKey(Constants.VERSION_KEY)) {
            throw new IllegalArgumentException("No version info");
        }*/

        String u = getUrl();
        URL url = URL.valueOf(u + "?" + getParameters());

        url = url.addParameters(serviceName2Map);

        boolean dynamic = isDynamic();
        if (!dynamic) {
            url = url.addParameter(Constants.DYNAMIC_KEY, false);
        }
        boolean enabled = isEnabled();
        if (enabled != url.getParameter("enabled", true)) {
            if (enabled) {
                url = url.removeParameter("enabled");
            } else {
                url = url.addParameter("enabled", false);
            }
        }

        return url;
    }

}
