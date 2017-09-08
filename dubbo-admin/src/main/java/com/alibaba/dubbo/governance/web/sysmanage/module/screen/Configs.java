/**
 * Project: dubbo.registry.console-2.1.0-SNAPSHOT
 * <p>
 * File Created at Sep 13, 2011
 * $Id: Configs.java 181723 2012-06-26 01:56:06Z tony.chenl $
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
package com.alibaba.dubbo.governance.web.sysmanage.module.screen;

import com.alibaba.dubbo.governance.service.ConfigService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Config;
import com.alibaba.dubbo.registry.common.domain.User;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ding.lid
 */
public class Configs extends Restful {

    @Autowired
    private ConfigService configDAO;

    @Autowired
    private HttpServletRequest request;

    public void index(Map<String, Object> context) {
        context.put("configs", configDAO.findAllConfigsMap());
    }

    public boolean update(Map<String, Object> context) {
        @SuppressWarnings("unchecked")
        Map<String, String[]> all = request.getParameterMap();
        ;
        if (all != null && all.size() > 0) {
            if (!User.ROOT.equals(currentUser.getRole())) {
                context.put("message", getMessage("HaveNoRootPrivilege"));
                return false;
            }
            List<Config> configs = new ArrayList<Config>();
            for (Map.Entry<String, String[]> entry : all.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();
                if (key != null && key.length() > 0 && !key.startsWith("_")) {
                    String value = "";
                    if (values != null && values.length > 0
                            && values[0] != null && values[0].length() > 0) {
                        value = values[0];
                    }
                    Config config = new Config();
                    config.setKey(key);
                    config.setUsername(currentUser.getUsername());
                    config.setOperatorAddress((String) context.get("operatorAddress"));
                    config.setValue(value);
                    configs.add(config);
                }
            }
            if (configs.size() > 0) {
                configDAO.update(configs);

                Set<String> usernames = new HashSet<String>();
                usernames.add(currentUser.getName());

                Map<String, Object> params = new HashMap<String, Object>();
                params.put("configs", configs);
            }
            return true;
        } else {
            context.put("message", getMessage("MissRequestParameters", "configKey,configValue"));
            return false;
        }
    }
}
