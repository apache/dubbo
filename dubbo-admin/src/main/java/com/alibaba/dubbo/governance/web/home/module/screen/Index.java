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

import com.alibaba.citrus.turbine.Context;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.pulltool.RootContextPath;
import com.alibaba.dubbo.registry.common.domain.Consumer;
import com.alibaba.dubbo.registry.common.domain.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Index
 *
 */
public class Index {
    // logger
    private static final Logger logger = LoggerFactory.getLogger(Index.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    public void execute(Context context) {
        Set<String> applications = new HashSet<String>();
        Set<String> services = new HashSet<String>();
        List<Provider> pList = new ArrayList<Provider>();
        try {
            pList = providerService.findAll();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        for (Provider p : pList) {
            applications.add(p.getApplication());
            services.add(p.getService());
        }
        List<Consumer> cList = new ArrayList<Consumer>();
        try {
            cList = consumerService.findAll();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        for (Consumer c : cList) {
            applications.add(c.getApplication());
            services.add(c.getService());
        }
        context.put("rootContextPath", new RootContextPath(request.getContextPath()));
        context.put("services", services.size());
        context.put("providers", pList.size());
        context.put("consumers", cList.size());
        context.put("applications", applications.size());
    }

}
