/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-4-9
 * $Id: Registry.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.domain;

import java.util.Date;

/**
 * Registry
 * 
 * @author william.liangf
 */
public class Registry extends Entity {

    private static final long serialVersionUID = -8866645978415551309L;

    private String registry;/* 注册中心地址 */
    
    private String url;
    
    private int connections = 0;/*注册中心连接数*/

    private Date expired;   /*过期时间*/
    
    private long alived;
    
    public Registry() {
    }

    public Registry(Long id) {
        super(id);
    }
    
    public Registry(String registryAddress, String consoleUrl, int aliveSeconds) {
        this.registry = registryAddress;
        this.url = consoleUrl;
        this.alived = aliveSeconds;
    }

    public String getAddress() {
        return registry;
    }

    public void setAddress(String registry) {
        this.registry = registry;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getExpired() {
        return expired;
    }

    public void setExpired(Date expiredDate) {
        this.expired = expiredDate;
    }

    public long getAlived() {
        return alived;
    }

    public void setAlived(long aliveSeconds) {
        this.alived = aliveSeconds;
    }

    
    public int getConnections() {
        return connections;
    }

    
    public void setConnections(int connections) {
        this.connections = connections;
    }

}
