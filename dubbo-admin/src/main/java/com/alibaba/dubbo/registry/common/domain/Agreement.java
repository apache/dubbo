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

/**
 * Specifies the applied Quality of Service Level Agreement (SLA) object.
 *
 */
public class Agreement extends Entity {

    private static final long serialVersionUID = -4888604682731513790L;

    private String service;   // service name

    private String consumerApplication; // application of consumer

    private long invocationQuantity; // call of the day

    private int tps; // TPS limit

    private int responseTime; // Response time in milliseconds

    private double availability; // ratio of available

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
