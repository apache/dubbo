/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-4-9
 * $Id: Consumer.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
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
 * @author william.liangf
 */
public class Consumer extends Entity {

    private static final long serialVersionUID = -1140894843784583237L;

    private String service; /* 消费者所引用的服务名称 */

    private String parameters;

    private String result;    /*路由结果*/

    private String address; /* 消费者地址 */

    private String registry; /* 消费者连接的注册中心地址 */

    private String application; /* 应用名 */

    private String username;      /* 消费者用户名 */

    private String statistics;    /* 服务调用统计信息 */

    private Date collected;  /* 服务调用统计时间 */

    private Override override;

    private List<Override> overrides;

    private List<Route> routes;

    private List<Provider> providers;

    private Date expired;   /*过期时间*/

    private long alived;    /*存活时间，单位秒*/

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
