/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.home.module.screen;

import com.alibaba.dubbo.governance.web.util.WebConstants;
import com.alibaba.dubbo.registry.common.domain.User;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class Shell {
    private static final Pattern OK_PATTERN = Pattern.compile("ok", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERROR_PATTERN = Pattern.compile("error", Pattern.CASE_INSENSITIVE);
    protected String role = null;
    protected String operator = null;
    protected User currentUser = null;
    protected String operatorAddress = null;
    @Autowired
    private HttpServletResponse response;

    private static String filterOK(String value) {
        if (value == null || value.length() == 0) {
            return value;
        }
        return OK_PATTERN.matcher(value).replaceAll("0k");
    }

    private static String filterERROR(String value) {
        if (value == null || value.length() == 0) {
            return value;
        }
        return ERROR_PATTERN.matcher(value).replaceAll("err0r");
    }

    public void execute(Map<String, Object> context) throws Exception {
        if (context.get(WebConstants.CURRENT_USER_KEY) != null) {
            User user = (User) context.get(WebConstants.CURRENT_USER_KEY);
            currentUser = user;
            operator = user.getUsername();
            role = user.getRole();
            context.put(WebConstants.CURRENT_USER_KEY, user);
        }
        operatorAddress = (String) context.get("request.remoteHost");
        context.put("operator", operator);
        context.put("operatorAddress", operatorAddress);
        try {
            String message = doExecute(context);
            context.put("message", "OK: " + filterERROR(message));
        } catch (Throwable t) {
            context.put("message", "ERROR: " + filterOK(t.getMessage()));
        }
        PrintWriter writer = response.getWriter();
        writer.print(context.get("message"));
        writer.flush();
    }

    protected abstract String doExecute(Map<String, Object> context) throws Exception;

}
