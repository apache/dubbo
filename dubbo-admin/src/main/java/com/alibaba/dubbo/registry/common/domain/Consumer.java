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
import com.alibaba.dubbo.common.utils.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Consumer
 *
 */
public class Consumer extends Entity {

    private static final long serialVersionUID = -1140894843784583237L;

    private String service; /* The name of the service referenced by the consumer */

    private String parameters;

    private String result;    /*route result*/

    private String address; /* address of consumer */

    private String registry; /* Consumer connected registry address */

    private String application; /* application name */

    private String username;      /* user name of consumer */

    private String statistics;    /* Service call statistics */

    private Date collected;  /* Date statistics was recorded */

    private Override override;

    private List<Override> overrides;

    private List<Route> routes;

    private List<Provider> providers;

    private Date expired;

    private long alived;    /*Time to live in milliseconds*/

    public Consumer() {
    }

    public Consumer(Long id) {
        super(id);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
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

    public String getStatistics() {
        return statistics;
    }

    public void setStatistics(String statistics) {
        this.statistics = statistics;
    }

    public Date getCollected() {
        return collected;
    }

    public void setCollected(Date collected) {
        this.collected = collected;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
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


    public void setAlived(long alived) {
        this.alived = alived;
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

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }

    public List<Provider> getProviders() {
        return providers;
    }

    public void setProviders(List<Provider> providers) {
        this.providers = providers;
    }

    public String toString() {
        return "Consumer [service=" + service + ", parameters=" + parameters + ", result=" + result
                + ", address=" + address + ", registry=" + registry + ", application="
                + application + ", username=" + username + ", statistics=" + statistics
                + ", collected=" + collected + ", routes=" + routes + ", overrides=" + overrides
                + ", expired=" + expired + ", alived=" + alived + "]";
    }

    public URL toUrl() {
        String group = null;
        String version = null;
        String path = service;
        int i = path.indexOf("/");
        if (i > 0) {
            group = path.substring(0, i);
            path = path.substring(i + 1);
        }
        i = path.lastIndexOf(":");
        if (i > 0) {
            version = path.substring(i + 1);
            path = path.substring(0, i);
        }
        Map<String, String> param = StringUtils.parseQueryString(parameters);
        param.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        if (group != null) {
            param.put(Constants.GROUP_KEY, group);
        }
        if (version != null) {
            param.put(Constants.VERSION_KEY, version);
        }
        return URL.valueOf(Constants.CONSUMER_PROTOCOL + "://" + address + "/" + path
                + "?" + StringUtils.toQueryString(param));
    }

}
