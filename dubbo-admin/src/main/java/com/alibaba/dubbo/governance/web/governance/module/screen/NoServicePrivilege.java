/**
 * Function:
 * <p>
 * File Created at 2010-11-17
 * $Id: NoServicePrivilege.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * <p>
 * Copyright 2009 Alibaba.com Croporation Limited.
 * All rights reserved.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.citrus.turbine.Context;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * TODO Comment of NoServicePrivilege
 *
 * @author guanghui.shigh
 */
public class NoServicePrivilege {

    @Autowired
    private HttpServletRequest request;

    public void execute(Context context) {
        context.put("returnUrl", request.getParameter("returnUrl"));
    }
}
