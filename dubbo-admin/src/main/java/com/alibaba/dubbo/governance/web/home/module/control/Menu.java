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
package com.alibaba.dubbo.governance.web.home.module.control;

import com.alibaba.citrus.service.requestcontext.parser.CookieParser;
import com.alibaba.citrus.turbine.Context;
import com.alibaba.dubbo.governance.sync.RegistryServerSync;
import com.alibaba.dubbo.governance.web.common.pulltool.RootContextPath;
import com.alibaba.dubbo.governance.web.util.WebConstants;
import com.alibaba.dubbo.registry.common.domain.User;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 */
public class Menu {

    @Autowired
    ServletContext servletcontext;
    @Autowired
    RegistryServerSync registryServerSync;
    @Autowired
    private HttpServletRequest request;

    public void execute(HttpSession session, Context context, CookieParser parser) {

        User user = (User) session.getAttribute(WebConstants.CURRENT_USER_KEY);
        if (user != null) context.put("operator", user.getUsername());

        RootContextPath rootContextPath = new RootContextPath(request.getContextPath());
        context.put("rootContextPath", rootContextPath);
        if (!context.containsKey("bucLogoutAddress")) {
            context.put("bucLogoutAddress", rootContextPath.getURI("logout"));
        }
        if (!context.containsKey("helpUrl")) {
            context.put("helpUrl", "http://code.alibabatech.com/wiki/display/dubbo");
        }
        context.put(WebConstants.CURRENT_USER_KEY, user);
        context.put("language", parser.getString("locale"));
        context.put("registryServerSync", registryServerSync);
    }
}
