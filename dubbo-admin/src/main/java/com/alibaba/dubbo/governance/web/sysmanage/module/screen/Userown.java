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
package com.alibaba.dubbo.governance.web.sysmanage.module.screen;

import com.alibaba.dubbo.governance.service.OwnerService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * Providers. URI: /services/$service/owners
 *
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
