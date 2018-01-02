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

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ProviderService;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Register extends Shell {

    @Autowired
    private ProviderService providerDAO;

    @Autowired
    private HttpServletRequest request;

//    @Autowired
//    private RegistryCache registryCache;

    @SuppressWarnings("unchecked")
    protected String doExecute(Map<String, Object> context) throws Exception {
        Map<String, String[]> params = request.getParameterMap();
        if (params == null || params.size() == 0) {
            throw new IllegalArgumentException("The url parameters is null! Usage: " + request.getRequestURL().toString() + "?com.xxx.XxxService=http://" + request.getRemoteAddr() + "/xxxService?application=xxx&foo1=123");
        }
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (entry.getKey() != null && entry.getKey().length() > 0
                    && entry.getValue() != null && entry.getValue().length > 0
                    && entry.getValue()[0] != null && entry.getValue()[0].length() > 0) {
                if (!currentUser.hasServicePrivilege(entry.getKey())) {
                    throw new IllegalStateException("The user " + currentUser.getUsername() + " have no privilege of service " + entry.getKey());
                }
                String serviceName = entry.getKey();
                Map<String, String> url2query = CollectionUtils.split(Arrays.asList(entry.getValue()), "?");
                // check whether url contain application info
                for (Map.Entry<String, String> e : url2query.entrySet()) {
                    Map<String, String> query = StringUtils.parseQueryString(e.getValue());
                    String app = query.get("application");
                    if (StringUtils.isBlank(app)) {
                        throw new IllegalStateException("No application for service(" + serviceName + "): "
                                + e.getKey() + "?" + e.getValue());
                    }
                }
                map.put(serviceName, url2query);
            }
        }
        if (map.size() > 0) {
//        	providerDAO.register(registryCache.getCurrentRegistry(), request.getRemoteAddr(), operatorAddress, operator, map, false, true);
        }
        return "Register " + map.size() + " services.";
    }
}
