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
package com.alibaba.dubbo.governance.web.personal.module.screen;

import com.alibaba.dubbo.governance.service.UserService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.User;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class Infos extends Restful {
    @Autowired
    private UserService userDAO;

    public void index(Map<String, Object> context) {
        User user = userDAO.findById(currentUser.getId());
        context.put("user", user);
    }

    public boolean update(Map<String, Object> context) {
        User user = new User();
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());
        user.setOperatorAddress(operatorAddress);
        user.setName((String) context.get("name"));
        user.setDepartment((String) context.get("department"));
        user.setEmail((String) context.get("email"));
        user.setPhone((String) context.get("phone"));
        user.setAlitalk((String) context.get("alitalk"));
        user.setLocale((String) context.get("locale"));
        userDAO.modifyUser(user);
        context.put("redirect", "../" + getClass().getSimpleName().toLowerCase());
        return true;
    }
}
