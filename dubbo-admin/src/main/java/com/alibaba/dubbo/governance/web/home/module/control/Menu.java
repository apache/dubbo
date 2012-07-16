/**
 * Function: 
 * 
 * File Created at 2010-11-17
 * $Id: Menu.java 185206 2012-07-09 03:06:37Z tony.chenl $
 * 
 * Copyright 2009 Alibaba.com Croporation Limited.
 * All rights reserved.
 */
package com.alibaba.dubbo.governance.web.home.module.control;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.citrus.service.requestcontext.parser.CookieParser;
import com.alibaba.citrus.turbine.Context;
import com.alibaba.dubbo.governance.sync.RegistryServerSync;
import com.alibaba.dubbo.governance.web.common.pulltool.RootContextPath;
import com.alibaba.dubbo.governance.web.util.WebConstants;
import com.alibaba.dubbo.registry.common.domain.User;

/**
 * @author guanghui.shigh
 * @author ding.lid
 * @author tony.chenl
 */
public class Menu {

    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    ServletContext servletcontext;
    
    @Autowired
    RegistryServerSync registryServerSync;

    public void execute(HttpSession session, Context context, CookieParser parser) {
        
        User user = (User) session.getAttribute(WebConstants.CURRENT_USER_KEY);
        if (user != null) context.put("operator", user.getUsername());
        
        RootContextPath rootContextPath = new RootContextPath(request.getContextPath());
        context.put("rootContextPath", rootContextPath);
        if (! context.containsKey("bucLogoutAddress")) {
        	context.put("bucLogoutAddress", rootContextPath.getURI("logout"));
        }
        if (! context.containsKey("helpUrl")) {
        	context.put("helpUrl", "http://code.alibabatech.com/wiki/display/dubbo");
        }
        context.put(WebConstants.CURRENT_USER_KEY, user);
        context.put("language", parser.getString("locale"));
        context.put("registryServerSync", registryServerSync);
    }
}
