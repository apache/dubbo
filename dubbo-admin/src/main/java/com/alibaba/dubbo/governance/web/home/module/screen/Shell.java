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
