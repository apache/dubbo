/**
 * Project: dubbo.registry.server-2.0.0-SNAPSHOT
 * 
 * File Created at Jan 24, 2011
 * $Id: RelateUserUtils.java 181723 2012-06-26 01:56:06Z tony.chenl $
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.common.utils;

import java.util.List;
import java.util.Set;

import com.alibaba.dubbo.governance.service.OwnerService;
import com.alibaba.dubbo.registry.common.route.ParseUtils;

/**
 * @author ding.lid
 */
public class RelateUserUtils {
    /**
     * 添加与服务相关的Owner
     * 
     * @param usernames 用于添加的用户名
     * @param serviceName 不含通配符
     */
    public static void addOwnersOfService(Set<String> usernames, String serviceName,
                                          OwnerService ownerDAO) {
        List<String> serviceNamePatterns = ownerDAO.findAllServiceNames();
        for (String p : serviceNamePatterns) {
            if (ParseUtils.isMatchGlobPattern(p, serviceName)) {
                List<String> list = ownerDAO.findUsernamesByServiceName(p);
                usernames.addAll(list);
            }
        }
    }

    /**
     * 添加与服务模式相关的Owner
     * 
     * @param usernames 用于添加的用户名
     * @param serviceNamePattern 服务模式，Glob模式
     */
    public static void addOwnersOfServicePattern(Set<String> usernames, String serviceNamePattern,
                                                OwnerService ownerDAO) {
        List<String> serviceNamePatterns = ownerDAO.findAllServiceNames();
        for (String p : serviceNamePatterns) {
            if (ParseUtils.hasIntersection(p, serviceNamePattern)) {
                List<String> list = ownerDAO.findUsernamesByServiceName(p);
                usernames.addAll(list);
            }
        }
    }
}
