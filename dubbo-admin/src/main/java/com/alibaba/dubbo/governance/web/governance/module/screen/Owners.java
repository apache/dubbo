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
package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.governance.service.OwnerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Owner;
import com.alibaba.dubbo.registry.common.util.Tool;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Providers. URI: /services/$service/owners
 *
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
                || username == null || username.length() == 0) {
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        if (!super.currentUser.hasServicePrivilege(service)) {
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
                || username == null || username.length() == 0) {
            context.put("message", getMessage("NoSuchOperationData"));
            return false;
        }
        if (!super.currentUser.hasServicePrivilege(service)) {
            context.put("message", getMessage("HaveNoServicePrivilege", service));
            return false;
        }
        ownerService.deleteOwner(owner);
        return true;
    }

}
