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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.governance.web.util.WebConstants;
import com.alibaba.dubbo.registry.common.domain.User;
import com.alibaba.fastjson.JSON;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public abstract class Restful {

    protected String role = null;
    protected String operator = null;

    //    @Autowired
//    RegistryValidator          registryService;
    protected User currentUser = null;
    protected String operatorAddress = null;
    protected URL url = null;
    @Autowired
    HttpServletRequest request;
    @Autowired
    private HttpServletResponse response;

    public void execute(Map<String, Object> context) throws Exception {
        Result result = new Result();
        if (request.getParameter("url") != null) {
            url = URL.valueOf(URL.decode(request.getParameter("url")));
        }
        if (context.get(WebConstants.CURRENT_USER_KEY) != null) {
            User user = (User) context.get(WebConstants.CURRENT_USER_KEY);
            currentUser = user;
            operator = user.getUsername();
            role = user.getRole();
            context.put(WebConstants.CURRENT_USER_KEY, user);
        }
        operatorAddress = (String) context.get("clientid");
        if (operatorAddress == null || operatorAddress.isEmpty()) {
            operatorAddress = (String) context.get("request.remoteHost");
        }
        context.put("operator", operator);
        context.put("operatorAddress", operatorAddress);
        String jsonResult = null;
        try {
            result = doExecute(context);
            result.setStatus("OK");
        } catch (IllegalArgumentException t) {
            result.setStatus("ERROR");
            result.setCode(3);
            result.setMessage(t.getMessage());
        }
//        catch (InvalidRequestException t) {
//            result.setStatus("ERROR");
//            result.setCode(2);
//            result.setMessage(t.getMessage());
//        }
        catch (Throwable t) {
            result.setStatus("ERROR");
            result.setCode(1);
            result.setMessage(t.getMessage());
        }
        response.setContentType("application/javascript");
        ServletOutputStream os = response.getOutputStream();
        try {
            jsonResult = JSON.toJSONString(result);
            os.print(jsonResult);
        } catch (Exception e) {
            response.setStatus(500);
            os.print(e.getMessage());
        } finally {
            os.flush();
        }
    }

    protected abstract Result doExecute(Map<String, Object> context) throws Exception;

}
