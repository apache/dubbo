/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.sysmanage.module.screen;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.governance.service.OwnerService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;

/**
 * Providers. URI: /services/$service/owners
 * 
 * @author william.liangf
 */
public class Userown extends Restful {

	@Autowired
	private OwnerService ownerDAO;

	public void index(Map<String, Object> context) {
		String user = (String) context.get("user");
		List<String> services;
		services = ownerDAO.findServiceNamesByUsername(user);
		context.put("user", user);
		context.put("services", services);
	}
}
