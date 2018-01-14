/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.governance.web.home.module.screen;

import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.registry.common.domain.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

public class Disable extends Shell {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private HttpServletRequest request;

    public void setProviderDAO(ProviderService providerDAO) {
        this.providerService = providerDAO;
    }

    protected String doExecute(Map<String, Object> context) throws Exception {
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
                if (!currentUser.hasServicePrivilege(provider.getService())) {
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
