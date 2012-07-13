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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.LoadBalance;
import com.alibaba.dubbo.registry.common.domain.Provider;
import com.alibaba.dubbo.registry.common.util.OverrideUtils;
import com.alibaba.dubbo.registry.common.util.Tool;

/**
 * Providers.
 * URI: /services/$service/loadbalances
 * 
 * @author william.liangf
 */
public class Loadbalances extends Restful {
    
    @Autowired
    private OverrideService overrideService;
    
    @Autowired
    private ProviderService providerService;

    public void index(Map<String, Object> context) {
        final String service = StringUtils.trimToNull((String) context.get("service"));
        
        List<LoadBalance> loadbalances;
        if (service != null && service.length() > 0) {
            loadbalances = OverrideUtils.overridesToLoadBalances(overrideService.findByService(service));
            
            loadbalances = OverrideUtils.overridesToLoadBalances(overrideService.findByService(service));
        } else {
            loadbalances = OverrideUtils.overridesToLoadBalances(overrideService.findAll());
        }
        context.put("loadbalances", loadbalances);
    }

    public void show(Long id, Map<String, Object> context) {
    	LoadBalance loadbalance = OverrideUtils.overrideToLoadBalance(overrideService.findById(id));
        context.put("loadbalance", loadbalance);
    }
    
    public void add(Map<String, Object> context) {
        String service = (String)context.get("service");
        if (service != null && service.length() > 0 && !service.contains("*")) {
            List<Provider> providerList = providerService.findByService(service);
            List<String> addressList = new ArrayList<String>();
            for(Provider provider : providerList){
                addressList.add(provider.getUrl().split("://")[1].split("/")[0]);
            }
            context.put("addressList", addressList);
            context.put("service", service);
            context.put("methods", CollectionUtils.sort(providerService.findMethodsByService(service)));
        } else {
            List<String> serviceList = Tool.sortSimpleName(providerService.findServices());
            context.put("serviceList", serviceList);
        }
        if(context.get("input") != null) context.put("input", context.get("input"));
    }

    public void edit(Long id, Map<String, Object> context) {
    	 add(context);
         show(id, context);
    }
    
    public boolean create(LoadBalance loadBalance, Map<String, Object> context) {
        if (!super.currentUser.hasServicePrivilege(loadBalance.getService())) {
            context.put("message", getMessage("HaveNoServicePrivilege", loadBalance.getService()));
            return false;
        }
        
    	loadBalance.setUsername((String)context.get("operator"));
    	overrideService.saveOverride(OverrideUtils.loadBalanceToOverride(loadBalance));
        return true;
    }

    
    public boolean update(LoadBalance loadBalance, Map<String, Object> context) {
        if (!super.currentUser.hasServicePrivilege(loadBalance.getService())) {
            context.put("message", getMessage("HaveNoServicePrivilege", loadBalance.getService()));
            return false;
        }
    	overrideService.updateOverride(OverrideUtils.loadBalanceToOverride(loadBalance));
        return true;
    }

    /**
     * 删除动作
     * @param ids
     * @return
     */
    public boolean delete(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            LoadBalance lb = OverrideUtils.overrideToLoadBalance(overrideService.findById(id));
            if (!super.currentUser.hasServicePrivilege(lb.getService())) {
                context.put("message", getMessage("HaveNoServicePrivilege", lb.getService()));
                return false;
            }
        }
        
        for (Long id : ids) {
        	overrideService.deleteOverride(id);
        }
        return true;
    }

}
