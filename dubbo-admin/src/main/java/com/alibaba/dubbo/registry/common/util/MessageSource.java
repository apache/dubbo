/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-7-7
 * <p>
 * Copyright 1999-2010 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.util;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * MessageSource
 *
 * @author william.liangf
 */
public class MessageSource {

    // 日志输出
    private static final Logger logger = LoggerFactory.getLogger(MessageSource.class);

    private final ResourceBundle resourceBundle;

    private final String errorPrefix;

    public MessageSource(ResourceBundle resourceBundle) {
        this(resourceBundle, null);
    }

    public MessageSource(ResourceBundle resourceBundle, String errorPrefix) {
        this.resourceBundle = resourceBundle;
        this.errorPrefix = errorPrefix == null ? "" : errorPrefix + " ";
    }

    public String getString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Throwable t) {
            logger.warn(errorPrefix + t.getMessage(), t);
            return key;
        }
    }

    public String getString(String key, Object... args) {
        try {
            return new MessageFormat(resourceBundle.getString(key)).format(args);
        } catch (Throwable t) {
            logger.warn(errorPrefix + t.getMessage(), t);
            return key;
        }
    }

}
