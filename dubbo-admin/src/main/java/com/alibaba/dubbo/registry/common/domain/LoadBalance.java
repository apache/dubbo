/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-10-21
 * $Id: LoadBalance.java 181192 2012-06-21 05:05:47Z tony.chenl $
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

/**
 * 负载均衡
 * @author rain.chenjr
 *
 */
public class LoadBalance extends Entity {

    public static final String ALL_METHOD = "*";
    private static final long serialVersionUID = -6050324375352581440L;
    private String service;            /* 服务名称 */

    private String method;             /* 方法名称 */

    private String strategy;              /*负载均衡策略*/

    private String username;              /*用户名*/

    public LoadBalance() {
    }

    public LoadBalance(Long id) {
        super(id);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


}
