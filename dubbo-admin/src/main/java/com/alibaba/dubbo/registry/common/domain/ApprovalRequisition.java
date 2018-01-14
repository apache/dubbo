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
 * Service online / offline approval
 *
 */
public class ApprovalRequisition extends Entity {
    private static final long serialVersionUID = -8778092807313048367L;

    private char operation; // Apply for action: add (C), modify (U), delete (D)

    private Long approvalId;

    private String service; // service name

    private String version;
    // wildcards, *，1.1.*
    // more than one, 1.1.3,1.1.5,2.*

    private boolean forProvider;

    private String machineList; // servcice machine
    // wildcards, 172.3.8.*
    // more thaan one, 172.1.9.8,172.1.9.123,172.3.3.*

    private String username;

    public char getOperation() {
        return operation;
    }

    public void setOperation(char operation) {
        this.operation = operation;
    }

    public Long getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(Long approvalId) {
        this.approvalId = approvalId;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isForProvider() {
        return forProvider;
    }

    public void setForProvider(boolean forProvider) {
        this.forProvider = forProvider;
    }

    public String getMachineList() {
        return machineList;
    }

    public void setMachineList(String machineList) {
        this.machineList = machineList;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
