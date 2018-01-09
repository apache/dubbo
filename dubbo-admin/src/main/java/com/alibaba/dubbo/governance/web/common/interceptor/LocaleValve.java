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

public class LocaleValve extends AbstractValve {

    //Add exceptions for interceptors
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

        // default chinese
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
            locale = "en";
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
