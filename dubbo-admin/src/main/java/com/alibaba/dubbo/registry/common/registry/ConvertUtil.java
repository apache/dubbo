/**
 * Project: dubbo.registry.common-2.2.0-SNAPSHOT
 * <p>
 * File Created at Mar 21, 2012
 * $Id: ConvertUtil.java 181192 2012-06-21 05:05:47Z tony.chenl $
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
package com.alibaba.dubbo.registry.common.registry;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ding.lid
 *
 */
public class ConvertUtil {
    private ConvertUtil() {
    }

    public static Map<String, Map<String, String>> convertRegister(Map<String, Map<String, String>> register) {
        Map<String, Map<String, String>> newRegister = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
            String serviceName = entry.getKey();
            Map<String, String> serviceUrls = entry.getValue();
            if (!serviceName.contains(":") && !serviceName.contains("/")) {
                for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                    String serviceUrl = entry2.getKey();
                    String serviceQuery = entry2.getValue();
                    Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                    String group = params.get("group");
                    String version = params.get("version");
                    params.remove("group");
                    params.remove("version");
                    String name = serviceName;
                    if (group != null && group.length() > 0) {
                        name = group + "/" + name;
                    }
                    if (version != null && version.length() > 0 && !"0.0.0".equals(version)) {
                        name = name + ":" + version;
                    }
                    Map<String, String> newUrls = newRegister.get(name);
                    if (newUrls == null) {
                        newUrls = new HashMap<String, String>();
                        newRegister.put(name, newUrls);
                    }
                    newUrls.put(serviceUrl, StringUtils.toQueryString(params));
                }
            } else {
                newRegister.put(serviceName, serviceUrls);
            }
        }
        return newRegister;
    }

    public static Map<String, String> convertSubscribe(Map<String, String> subscribe) {
        Map<String, String> newSubscribe = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : subscribe.entrySet()) {
            String serviceName = entry.getKey();
            String serviceQuery = entry.getValue();
            if (!serviceName.contains(":") && !serviceName.contains("/")) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String group = params.get("group");
                String version = params.get("version");
                params.remove("group");
                params.remove("version");
                String name = serviceName;
                if (group != null && group.length() > 0) {
                    name = group + "/" + name;
                }
                if (version != null && version.length() > 0 && !"0.0.0".equals(version)) {
                    name = name + ":" + version;
                }
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        }
        return newSubscribe;
    }

    public static Map<String, String> serviceName2Map(String serviceName) {
        String group = null;
        String version = null;
        int i = serviceName.indexOf("/");
        if (i > 0) {
            group = serviceName.substring(0, i);
            serviceName = serviceName.substring(i + 1);
        }
        i = serviceName.lastIndexOf(":");
        if (i > 0) {
            version = serviceName.substring(i + 1);
            serviceName = serviceName.substring(0, i);
        }

        Map<String, String> ret = new HashMap<String, String>();
        if (!StringUtils.isEmpty(serviceName)) {
            ret.put(Constants.INTERFACE_KEY, serviceName);
        }
        if (!StringUtils.isEmpty(version)) {
            ret.put(Constants.VERSION_KEY, version);
        }
        if (!StringUtils.isEmpty(group)) {
            ret.put(Constants.GROUP_KEY, group);
        }

        return ret;
    }
}
