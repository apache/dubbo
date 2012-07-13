/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-4-9
 * $Id: Entity.java 181192 2012-06-21 05:05:47Z tony.chenl $
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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Entity
 * 
 * @author william.liangf
 */
public abstract class Entity implements Serializable {

	private static final long serialVersionUID = -3031128781434583143L;
	
	private List<Long> ids;

	private Long id;

	private Date created;

    private Date modified;
    
    private Date now;
    
    private String operator;
    
    private String operatorAddress;
    
    private boolean miss;
    
    public Entity() {
    }
    
    public Entity(Long id) {
        this.id = id;
    }
    
	public List<Long> getIds() {
		return ids;
	}

	public void setIds(List<Long> ids) {
		this.ids = ids;
	}

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

	public Date getNow() {
		return now;
	}

	public void setNow(Date now) {
		this.now = now;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		if (operator != null && operator.length() > 200) {
		    operator = operator.substring(0, 200);
    	}
		this.operator = operator;
	}
    
    public String getOperatorAddress() {
        return operatorAddress;
    }
    
    public void setOperatorAddress(String operatorAddress) {
        this.operatorAddress = operatorAddress;
    }

    public boolean isMiss() {
        return miss;
    }
    
    public void setMiss(boolean miss) {
        this.miss = miss;
    }

}
