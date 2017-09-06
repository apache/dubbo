/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.home.module.screen;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.registry.common.domain.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author tony.chenl
 */
public class Unregister extends Shell {

    @Autowired
    private ProviderService providervice;

    @Autowired
    private HttpServletRequest request;

    @SuppressWarnings("unchecked")
    protected String doExecute(Map<String, Object> context) throws Exception {
        Map<String, String[]> params = request.getParameterMap();
        if (params == null || params.size() == 0) {
            throw new IllegalArgumentException("The url parameters is null! Usage: " + request.getRequestURL().toString() + "?com.xxx.XxxService=http://" + operatorAddress + "/xxxService");
        }
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (entry.getKey() != null && entry.getKey().length() > 0
                    && entry.getValue() != null && entry.getValue().length > 0
                    && entry.getValue()[0] != null && entry.getValue()[0].length() > 0) {
                if (!currentUser.hasServicePrivilege(entry.getKey())) {
                    throw new IllegalStateException("The user " + operator + " have no privilege of service " + entry.getKey());
                }
                for (Entry<String, String> e : CollectionUtils.split(Arrays.asList(entry.getValue()), "?").entrySet()) {
                    Provider provider = providervice.findByServiceAndAddress(entry.getKey(), e.getKey());
                    if (provider != null) {
                        providervice.deleteStaticProvider(provider.getId());
                    }
                }
            }
        }

        return "Unregister " + params.size() + " services.";
    }

}
