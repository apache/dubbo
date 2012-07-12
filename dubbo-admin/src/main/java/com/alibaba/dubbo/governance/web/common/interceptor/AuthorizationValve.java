/**
 * Function: 拦截器
 * 
 * File Created at 2011-08-11
 * 
 * Copyright 2011 Alibaba.com Croporation Limited.
 * All rights reserved.
 */
package com.alibaba.dubbo.governance.web.common.interceptor;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.citrus.service.pipeline.PipelineContext;
import com.alibaba.citrus.service.pipeline.support.AbstractValve;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.governance.web.util.WebConstants;
import com.alibaba.dubbo.registry.common.domain.User;

/**
 * @author william.liangf
 * @author guanghui.shigh
 * @author ding.lid
 * @author tony.chenl
 */
public class AuthorizationValve extends AbstractValve {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationValve.class);

    @Autowired
    private HttpServletRequest  request;

    @Override
    protected void init() throws Exception {
    }

    public void invoke(PipelineContext pipelineContext) throws Exception {
        if (logger.isInfoEnabled()) {
            logger.info("AuthorizationValve of uri: " + request.getRequestURI());
        }
        //FIXME
        if(!request.getRequestURI().startsWith("/status/")){
            User user = new User();
            user.setUsername("admin");
            user.setName("管理员");
            user.setRole(User.ROOT);
            user.setEnabled(true);
            user.setCreator("dubbo");
            user.setLocale("zh");
            user.setServicePrivilege("*");
            request.getSession().setAttribute(WebConstants.CURRENT_USER_KEY, user);
            pipelineContext.invokeNext();
        }else{
            pipelineContext.invokeNext();
        }
    }

}
