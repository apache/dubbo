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
package org.apache.dubbo.config.dynamic.parser;

import com.alibaba.fastjson.JSON;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.dynamic.parser.model.ConfigItem;
import org.apache.dubbo.config.dynamic.parser.model.ConfiguratorRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ConfigParser {

    public static List<URL> parseConfigurators(String rawConfig) {
        List<URL> urls = new ArrayList<>();
        ConfiguratorRule configuratorRule = JSON.parseObject(rawConfig, ConfiguratorRule.class);
        List<ConfigItem> items = configuratorRule.getItems();
        for (ConfigItem item : items) {
            StringBuilder sb = new StringBuilder();
            sb.append("?category=");
            sb.append(Constants.DYNAMIC_CONFIGURATORS_CATEGORY);
            sb.append("&side=");
            sb.append(item.getSide());
            sb.append("&application=");
            sb.append(item.getApp());
            List<Map<String, String>> rules = item.getRules();
            for (Map<String, String> rule : rules) {
                sb.append("&");
                sb.append(rule.get("key"));
                sb.append("=");
                sb.append(rule.get("value"));
            }
            if (configuratorRule.getGroup() != null) {
                sb.append("&group=");
                sb.append(configuratorRule.getGroup());
            }
            if (configuratorRule.getVersion() != null) {
                sb.append("&version=");
                sb.append(configuratorRule.getVersion());
            }
            List<String> addresses = item.getAddresses();
            for (String addr : addresses) {
                urls.add(URL.valueOf("override://" + addr + "/" + configuratorRule.getInterfaceName() + sb.toString()));
            }
        }
        return urls;
    }

    public static List<URL> parseRouters(String rawConfig) {
        List<URL> urls = new ArrayList<>();
        urls.add(URL.valueOf(""));
        return urls;
    }

}
