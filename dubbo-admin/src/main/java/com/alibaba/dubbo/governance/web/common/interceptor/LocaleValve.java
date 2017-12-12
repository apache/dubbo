/**
 * Project: dubbo.registry.console-2.1.0-SNAPSHOT
 * <p>
 * File Created at Sep 13, 2011
 * $Id: LocaleValve.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * <p>
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.common.interceptor;

import com.alibaba.citrus.service.pipeline.PipelineContext;
import com.alibaba.citrus.service.pipeline.support.AbstractValve;
import com.alibaba.citrus.turbine.TurbineRunData;
import com.alibaba.dubbo.governance.web.common.i18n.LocaleUtil;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static com.alibaba.citrus.turbine.util.TurbineUtil.getTurbineRunData;

/**
 * @author guanghui.shigh
 */
public class LocaleValve extends AbstractValve {

    //添加拦截器例外设置
    private final static Set<String> TARGET_WITHOUT_CHECK = new HashSet<String>();

    static {
        TARGET_WITHOUT_CHECK.add("/ok");
        TARGET_WITHOUT_CHECK.add("/error");
        TARGET_WITHOUT_CHECK.add("/login");
        TARGET_WITHOUT_CHECK.add("/logout");
    }

    @Autowired
    private HttpServletRequest request;

    private boolean ignoreTarget(String target) {
        return TARGET_WITHOUT_CHECK.contains(target);
    }

    @Override
    protected void init() throws Exception {
    }

    public void invoke(PipelineContext pipelineContext) throws Exception {
        TurbineRunData rundata = getTurbineRunData(request);
        if (ignoreTarget(rundata.getTarget())) {
            pipelineContext.invokeNext();
            return;
        }

        //默认是中文
        String[] temp = rundata.getCookies().getStrings("locale");
        String locale = null;
        if (temp != null) {
            if (temp.length > 1) {
                locale = temp[temp.length - 1];
            } else if (temp.length == 1) {
                locale = temp[0];
            }
        }
        if (locale == null || "".equals(locale)) {
            locale = "zh";
        }

        Locale newLocale = Locale.SIMPLIFIED_CHINESE;
        if ("en".equals(locale)) {
            newLocale = Locale.ENGLISH;
        } else if ("zh".equals(locale)) {
            newLocale = Locale.SIMPLIFIED_CHINESE;
        } else if ("zh_TW".equals(locale)) {
            newLocale = Locale.TRADITIONAL_CHINESE;
        }
        LocaleUtil.setLocale(newLocale);

        pipelineContext.invokeNext();
    }
}
