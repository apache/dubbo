/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.governance.service.OwnerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Owner;
import com.alibaba.dubbo.registry.common.util.Tool;

/**
 * Providers. URI: /services/$service/owners
 * 
 * @author william.liangf
 */
public class Owners extends Restful {

	@Autowired
	private OwnerService ownerService;

	@Autowired
	private ProviderService providerService;
	
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
		String service = (String) context.get("service");
		if (service == null || service.length() == 0) {
            List<String> serviceList = Tool.sortSimpleName(new ArrayList<String>(providerService.findServices()));
            context.put("serviceList", serviceList);
        }
	}

	public boolean create(Owner owner, Map<String, Object> context) {
		String service = owner.getService();
		String username = owner.getUsername();
		if (service == null || service.length() == 0
        		|| username == null || username.length() == 0){
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        if (! super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
		ownerService.saveOwner(owner);
		return true;
	}

	public boolean delete(Long[] ids, Map<String, Object> context) {
		String service = (String) context.get("service");
		String username = (String) context.get("username");
		Owner owner = new Owner();
		owner.setService(service);
		owner.setUsername(username);
		if (service == null || service.length() == 0
        		|| username == null || username.length() == 0){
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        if (! super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
		ownerService.deleteOwner(owner);
		return true;
	}

}
