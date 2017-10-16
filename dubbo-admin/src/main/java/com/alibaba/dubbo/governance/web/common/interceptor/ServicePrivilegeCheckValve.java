/**
 * Function: 拦截器
 * <p>
 * File Created at 2011-08-11
 * <p>
 * Copyright 2011 Alibaba.com Croporation Limited.
 * All rights reserved.
 */
package com.alibaba.dubbo.governance.web.common.interceptor;

import com.alibaba.citrus.service.pipeline.PipelineContext;
import com.alibaba.citrus.service.pipeline.support.AbstractValve;
import com.alibaba.citrus.turbine.TurbineRunData;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.governance.web.util.WebConstants;
import com.alibaba.dubbo.registry.common.domain.User;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

import static com.alibaba.citrus.turbine.util.TurbineUtil.getTurbineRunData;

/**
 * @author guanghui.shigh
 */
public class ServicePrivilegeCheckValve extends AbstractValve {
    private static final Logger logger = LoggerFactory.getLogger(ServicePrivilegeCheckValve.class);

    @Autowired
    private HttpServletRequest request;

    @Override
    protected void init() throws Exception {
    }

    public void invoke(PipelineContext pipelineContext) throws Exception {
        User user = (User) request.getSession().getAttribute(WebConstants.CURRENT_USER_KEY);
        invokeCheckServicePrivilege(user);
        pipelineContext.invokeNext();
    }


    private void invokeCheckServicePrivilege(User user) {
        TurbineRunData rundata = getTurbineRunData(request);
        HttpSession session = request.getSession();

        @SuppressWarnings("unchecked")
        Map<String, String[]> requestMapping = request.getParameterMap();

        //记录上次操作到请求中
        String returnURL = "";
        if (session.getAttribute("returnURL") == null) {
            returnURL = request.getContextPath();
        } else {
            returnURL = (String) session.getAttribute("returnURL");
        }

        if (requestMapping.get("service").length > 0) {
            String service = ((String[]) requestMapping.get("service"))[0];
            String method = "index";
            if (requestMapping.get("_method").length > 0) {
                method = requestMapping.get("_method")[0];
            }

            boolean exclude = "index".equals(method) || "show".equals(method);
            if (!exclude) {
                if (user != null && !user.hasServicePrivilege(service)) {
                    request.setAttribute("returnURL", returnURL);
                    redirectToNoRight(rundata);
                }
            }
        }
        String type = requestMapping.get("_type").length == 0 ? null : requestMapping.get("_type")[0];
        if (!"noServicePrivilege".equals(type)) {
            session.setAttribute("returnURL", request.getRequestURI());
        }
        return;
    }

    /**
     * 无权限跳转
     * @param rundata
     */
    private void redirectToNoRight(TurbineRunData rundata) {
        if (logger.isInfoEnabled()) {
            logger.info("No right to access: " + request.getRequestURI());
        }

        rundata.getParameters().setString("returnURL1", (String) rundata.getRequest().getAttribute("returnURL"));
        rundata.setRedirectLocation("http://localhost:8080/governance/noServicePrivilege?returnURL=" + rundata.getRequest().getAttribute("returnURL"));
        return;
    }
}
