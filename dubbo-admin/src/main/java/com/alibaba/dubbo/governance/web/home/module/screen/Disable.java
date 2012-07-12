/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.home.module.screen;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.registry.common.domain.Provider;

/**
 * @author tony.chenl
 */
public class Disable extends Shell {

    @Autowired
    private ProviderService  providerService;
    
    @Autowired
    private HttpServletRequest request;

    public void setProviderDAO(ProviderService providerDAO) {
        this.providerService = providerDAO;
    }

    protected String doExecute(Map<String,Object> context) throws Exception {
    	String address = request.getParameter("provider");
    	if (address == null || address.length() == 0) {
    	    address = request.getParameter("client");
    	}
		if (address == null || address.length() == 0) {
			throw new IllegalArgumentException("The url provider parameter is null! Usage: " + request.getRequestURL().toString() + "?provider=" + operatorAddress);
		}
		List<Provider> providers = providerService.findByAddress(address);
		if (providers != null && providers.size() > 0) {
			for (Provider provider : providers) {
				if (! currentUser.hasServicePrivilege(provider.getService())) {
					throw new IllegalStateException("The user " + currentUser.getUsername() + " have no privilege of service " + provider.getService());
				}
			}
			for (Provider provider : providers) {
				provider.setUsername(operator);
				provider.setOperatorAddress(operatorAddress);
				providerService.disableProvider(provider.getId());
			}
		}
		return "Disable " + (providers == null ? 0 : providers.size()) + " services.";
    }

}
