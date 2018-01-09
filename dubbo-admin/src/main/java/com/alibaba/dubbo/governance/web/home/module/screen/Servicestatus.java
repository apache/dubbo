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

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

public class Servicestatus {
//    @Autowired
//    private RegistryCache registryCache;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private ProviderService providerDAO;

    @Autowired
    private HttpServletResponse response;

    public void execute(Map<String, Object> context) throws Exception {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !"/".equals(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri.startsWith("/status/")) {
            uri = uri.substring("/status/".length());
        }
//        Map<String, String> providers = registryCache.getServices().get(uri);
//        if (providers == null || providers.size() == 0) {
//            providers = providerDAO.lookup(uri);
//        }
//        if (providers == null || providers.size() == 0) {
//            context.put("message", "ERROR"
//                        + new SimpleDateFormat(" [yyyy-MM-dd HH:mm:ss] ").format(new Date())
//                        + Status.filterOK("No such any provider for service " + uri));
//        } else {
//            context.put("message", "OK");
//        }
        PrintWriter writer = response.getWriter();
        writer.print(context.get("message").toString());
        writer.flush();
    }
}
