/**
 * Project: iris-web-1.0-SNAPSHOT
 * <p>
 * File Created at Jul 9, 2010
 * $Id: I18nMessageTool.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.common.pulltool;

import com.alibaba.citrus.service.pull.ToolFactory;
import com.alibaba.dubbo.governance.biz.common.i18n.MessageResourceService;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * PullTool for accessing message bundle.
 *
 * @author gerry
 */
public class I18nMessageTool implements ToolFactory {

    @Autowired
    private MessageResourceService messageResourceService;
    private boolean singleton = true; //应该是global范围的对象！！

    public Object createTool() throws Exception {
        return messageResourceService;
    }

    public boolean isSingleton() {
        return this.singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

}
