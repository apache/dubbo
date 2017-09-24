/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-7-14
 * $Id: Agreement.java 181192 2012-06-21 05:05:47Z tony.chenl $
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
 * 指定应用的服务质量等级协定(SLA)对象。
 *
 * @author rain.chenjr
 * @author ding.lid
 */
public class Agreement extends Entity {

    private static final long serialVersionUID = -4888604682731513790L;

    private String service;   //服务名

    private String consumerApplication; // 服务消费者应用名

    private long invocationQuantity; // 一天调用量

    private int tps; // TPS上限

    private int responseTime; // 响应时间，毫秒

    private double availability; // 可用率

    private String username;

    public Agreement() {
    }

    public Agreement(Long id) {
        super(id);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getConsumerApplication() {
        return consumerApplication;
    }

    public void setConsumerApplication(String consumerApplication) {
        this.consumerApplication = consumerApplication;
    }

    public long getInvocationQuantity() {
        return invocationQuantity;
    }

    public void setInvocationQuantity(long invocationQuantity) {
        this.invocationQuantity = invocationQuantity;
    }

    public int getTps() {
        return tps;
    }

    public void setTps(int tps) {
        this.tps = tps;
    }

    public int getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }

    public double getAvailability() {
        return availability;
    }

    public void setAvailability(double availability) {
        this.availability = availability;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
