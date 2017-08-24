/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-5-14
 * <p>
 * Copyright 1999-2010 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.home.module.screen;

/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */

import com.alibaba.dubbo.governance.service.ProviderService;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @author tony.chenl
 */
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
