/**
 * Project: dubbo.registry.server-2.1.0-SNAPSHOT
 * 
 * File Created at Sep 1, 2011
 * $Id: Layer.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.domain;

/**
 * @author ding.lid
 */
public class Layer extends Entity {
    private static final long serialVersionUID = 6114868933223039253L;

    private String            username;

    private String            arch;
    private String            name;
    private int               value;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
    
    public Layer(Long id) {
        super(id);
    }
    
    public Layer() {
    }
}
