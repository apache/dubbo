/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.home.module.screen;

import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.governance.web.util.WebConstants;
import com.alibaba.dubbo.registry.common.domain.User;
import com.alibaba.fastjson.JSON;

public abstract class Restful {

    @Autowired
    private HttpServletResponse response;
    
    @Autowired
    HttpServletRequest request;
    
//    @Autowired
//    RegistryValidator          registryService;

    protected String            role            = null;
    protected String            operator        = null;
    protected User              currentUser     = null;
    protected String            operatorAddress = null;
    protected URL               url = null;

    public void execute(Map<String, Object> context) throws Exception {
        Result result = new Result();
        if(request.getParameter("url")!=null){
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
        if(operatorAddress==null || operatorAddress.isEmpty()){
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
        catch (Throwable t){
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
        }finally{
            os.flush();
        }
    }

    protected abstract Result doExecute(Map<String, Object> context) throws Exception;

}
