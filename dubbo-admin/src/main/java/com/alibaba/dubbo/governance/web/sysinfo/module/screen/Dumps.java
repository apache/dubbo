/*
 * Copyright 1999-2101 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.governance.web.sysinfo.module.screen;

import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author tony.chenl
 */
public class Dumps extends Restful {

    @Autowired
    ProviderService providerDAO;

    @Autowired
    ConsumerService consumerDAO;

    @Autowired
    HttpServletResponse response;

    public void index(Map<String, Object> context) {
        context.put("noProviderServices", getNoProviders());
        context.put("services", providerDAO.findServices());
        context.put("providers", providerDAO.findAll());
        context.put("consumers", consumerDAO.findAll());
    }

    private List<String> getNoProviders() {
        List<String> providerServices = providerDAO.findServices();
        List<String> consumerServices = consumerDAO.findServices();
        List<String> noProviderServices = new ArrayList<String>();
        if (consumerServices != null) {
            noProviderServices.addAll(consumerServices);
            noProviderServices.removeAll(providerServices);
        }
        return noProviderServices;
    }
}
