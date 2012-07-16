/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.governance.service.OwnerService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Owner;

/**
 * Providers. URI: /services/$service/owners
 * 
 * @author william.liangf
 */
public class Owners extends Restful {

	@Autowired
	private OwnerService ownerService;
	
	public void index(Map<String, Object> context) {
		String service = (String) context.get("service");
		List<Owner> owners;
		if (service != null && service.length() > 0) {
			owners = ownerService.findByService(service);
		} else {
			owners = ownerService.findAll();
		}
		context.put("owners", owners);
	}
	
	public void add(Map<String, Object> context) {
	}

	public void create(Owner owner, Map<String, Object> context) {
		owner.getUsername();
	}

}
