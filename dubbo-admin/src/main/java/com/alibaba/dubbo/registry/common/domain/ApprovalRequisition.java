/**
 * Project: dubbo.registry.server-2.1.0-SNAPSHOT
 * <p>
 * File Created at Sep 14, 2011
 * $Id: ApprovalRequisition.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * <p>
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
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
 * 服务上线申请。
 *
 * @author ding.lid
 */
public class ApprovalRequisition extends Entity {
    private static final long serialVersionUID = -8778092807313048367L;

    private char operation; // 申请操作：新增（C），修改（U），删除（D） 

    private Long approvalId;

    private String service; // 服务名称

    private String version;
    // 可以使用通配符， *，1.1.*
    // 可以包含多个 1.1.3,1.1.5,2.*

    private boolean forProvider;

    private String machineList; // 服务所在机器
    // 可以使用通配符 172.3.8.*
    // 可以有多个 172.1.9.8,172.1.9.123,172.3.3.*

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
