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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Consumer;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.domain.Route;
import com.alibaba.dubbo.registry.common.route.RouteUtils;
import com.alibaba.dubbo.registry.common.util.Tool;

/**
 * Consumers. URI: /services/$service/consumers
 * 
 * @author william.liangf
 */
public class Consumers extends Restful {
    
    @Autowired
    private ConsumerService consumerService;
    
    @Autowired
    private OverrideService overrideService;
    
    @Autowired
    private RouteService routeService;
    
    public void index(Map<String, Object> context) throws Exception {
        String service = (String) context.get("service");
        String application = (String) context.get("application");
        String address = (String) context.get("address");
        List<Consumer> consumers;
        List<Override> overrides;
        // service
        if (service != null && service.length() > 0) {
            consumers = consumerService.findByService(service);
            overrides = overrideService.findByService(service);
        }
        // address
        else if (address != null && address.length() > 0) {
            consumers = consumerService.findByAddress(address);
            overrides = overrideService.findByAddress(Tool.getIP(address));
        }
        // application
        else if (application != null && application.length() > 0) {
            consumers = consumerService.findByApplication(application);
            overrides = overrideService.findByApplication(application);
        }
        // all
        else {
            consumers = consumerService.findAll();
            overrides = overrideService.findAll();
        }
        if (consumers != null && consumers.size() > 0) {
            for (Consumer consumer : consumers) {
                Map<String, Route> findUsedRoute = RouteUtils.findUsedRoute(consumer.getService(), consumer.getAddress(), consumer.getParameters(),
routeService.findByService(consumer.getService()), null);
                if(findUsedRoute.size() > 0){
                	 consumer.setMethod2Route(findUsedRoute);
                }
                if (overrides != null && overrides.size() > 0) {
                    for (Override override : overrides) {
                        if (consumer.getService().equals(override.getService()) 
                                && ("*".equals(override.getAddress()) || (consumer.getAddress() != null && Tool.getIP(consumer.getAddress()).equals(override.getAddress())))
                                && ("*".equals(override.getApplication()) || (consumer.getApplication() != null && consumer.getApplication().equals(override.getApplication())))) {
                            consumer.setOverride(override);
                        }
                    }
                }
            }
        }
        context.put("consumers", consumers);
    }
    
    public void show(Long id, Map<String, Object> context) {
        Consumer consumer = consumerService.findConsumer(id);
        context.put("consumer", consumer);
        
        Map<String, Route> findUsedRoute = RouteUtils.findUsedRoute(consumer.getService(), consumer.getAddress(), consumer.getParameters(),
                routeService.findByService(consumer.getService()), null);
        if(findUsedRoute.size() > 0){
       	 consumer.setMethod2Route(findUsedRoute);
       }
    }

    // FIXME 实现Mock！注意处理修改后的解决延时问题！！
    public boolean mock(Long[] ids, Map<String, Object> context) {
        if (ids == null || ids.length == 0){
            context.put("message", getMessage("Id is not exist"));
            return false;
        }
        String mock = (String) context.get("mock");
        List<Consumer> consumers = new ArrayList<Consumer>();
        for (Long id : ids) {
            Consumer c = consumerService.findConsumer(id);
            if(c != null){
                consumers.add(c);
                if (!super.currentUser.hasServicePrivilege(c.getService())) {
                    context.put("message", getMessage("HaveNoServicePrivilege", c.getService()));
                    return false;
                }
            }
        }
        for(Consumer consumer : consumers) {
            String service = consumer.getService();
            String address = Tool.getIP(consumer.getAddress());
            List<Override> overrides = overrideService.findByServiceAndAddress(service, address);
            if (overrides != null && overrides.size() > 0) {
                for (Override override: overrides) {
                    Map<String, String> map = StringUtils.parseQueryString(override.getParams());
                    if (mock == null || mock.length() == 0) {
                        map.remove("mock");
                    } else {
                        map.put("mock", URL.encode(mock));
                    }
                    override.setParams(StringUtils.toQueryString(map));
                    override.setEnabled(true);
                    override.setOperator(operator);
                    override.setOperatorAddress(operatorAddress);
                    overrideService.updateOverride(override);
                }
            } else if (mock != null && mock.length() > 0) {
                Override override = new Override();
                override.setService(service);
                override.setAddress(address);
                override.setParams("mock=" + URL.encode(mock));
                override.setEnabled(true);
                override.setOperator(operator);
                override.setOperatorAddress(operatorAddress);
                overrideService.saveOverride(override);
            }
        }
        return true;
    }
}
