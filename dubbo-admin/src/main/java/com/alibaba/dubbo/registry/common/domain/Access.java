/**
 * Project: dubbo.registry.server-2.0.0-SNAPSHOT
 * <p>
 * File Created at 2010-7-14
 * $Id: Access.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * <p>
 * Copyright 2010 Alibaba.com Croporation Limited.
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
 *
 * @author william.liangf
 */
public class Access extends Entity {

    private static final long serialVersionUID = -962351722638094446L;

    private String service;      /*服务名*/

    private String address;  /*消费者地址*/

    private boolean allow;           /*状态*/

    private String username;         /*用户名*/

    public Access() {
    }

    public Access(Long id) {
        super(id);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isAllow() {
        return allow;
    }

    public void setAllow(boolean allow) {
        this.allow = allow;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
