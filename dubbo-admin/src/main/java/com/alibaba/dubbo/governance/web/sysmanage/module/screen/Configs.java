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
