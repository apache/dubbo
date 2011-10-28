/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: InformationProvider.java 34672 2010-01-19 06:25:44Z william.liangf $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.container.page;

import java.util.Map;

/**
 * InformationProvider
 * 
 * @author william.liangf
 */
public interface PageFactory {

    String getUri();

    String getName();

    String getDescription();

    Page getPage(Map<String, String> params);

}
