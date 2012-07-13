/**
 * 
 * Copyright 1999-2011 Alibaba.com Croporation Limited.
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
 * Mock
 * 
 * @author guanghui.shigh
 */
public class Mock extends Entity {

    private static final long serialVersionUID = 872527738197173003L;
    
    private String            name;
    
    private String            service;

    private String            method;

    private String            parameters;

    private boolean           exception;

    private String            result;

    private String            username;

    private boolean           autoRun;
    
    private String            consumerAddr;
    
    public Mock() {
    }

    public Mock(Long id) {
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

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

	public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

	public boolean isException() {
		return exception;
	}

	public void setException(boolean exception) {
		this.exception = exception;
	}

	public boolean isAutoRun() {
		return autoRun;
	}

	public void setAutoRun(boolean autoRun) {
		this.autoRun = autoRun;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getConsumerAddr() {
		return consumerAddr;
	}

	public void setConsumerAddr(String consumerAddr) {
		this.consumerAddr = consumerAddr;
	}

}
