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
package org.apache.dubbo.registry.integration.parser;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.integration.parser.model.ConfigItem;
import org.apache.dubbo.registry.integration.parser.model.ConfiguratorConfig;
import org.apache.dubbo.registry.integration.parser.model.ConfiguratorRule;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class ConfigParser {

    public static List<URL> parseConfigurators(String rawConfig) {
        List<URL> urls = new ArrayList<>();
        ConfiguratorConfig configuratorConfig = parseObject(rawConfig, ConfiguratorConfig.class);
        String scope = configuratorConfig.getScope();
        List<ConfigItem> items = configuratorConfig.getConfigs();

        if (ConfiguratorConfig.SCOPE_APPLICATION.equals(scope)) {
            items.forEach(item -> urls.addAll(
                    appItemToUrls(item, configuratorConfig.getKey())
                            .stream()
                            .map(u -> u.addParameter(Constants.CATEGORY_KEY, Constants.APP_DYNAMIC_CONFIGURATORS_CATEGORY))
                            .collect(Collectors.toList())
            ));
        } else { // servcie scope by default.
            items.forEach(item -> urls.addAll(serviceItemToUrls(item, configuratorConfig.getKey())));
        }
        return urls;
    }

    public static <T> T parseObject(String rawConfig, Class<T> clazz) {
        Constructor constructor = new Constructor(clazz);
        TypeDescription itemDescription = new TypeDescription(clazz);
        itemDescription.addPropertyParameters("items", ConfigItem.class);
        constructor.addTypeDescription(itemDescription);

        Yaml yaml = new Yaml(constructor);
        return yaml.load(rawConfig);
    }

    private static List<URL> serviceItemToUrls(ConfigItem item, String serviceKey) {
        List<URL> urls = new ArrayList<>();
        List<String> addresses = item.getAddresses();
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        if (addresses.size() == 0) {
            addresses.add(Constants.ANYHOST_VALUE);
        }

        addresses.forEach(addr -> {
            String urlStr = "override://" + addr + "/";
            List<String> apps = item.getApplications();
            if (apps != null && apps.size() > 0) {
                apps.forEach(app -> {
                    urls.add(URL.valueOf(urlStr + appendService(serviceKey) + toParameterString(item) + "&application=" + app));
                });
            } else {
                urls.add(URL.valueOf(urlStr + appendService(serviceKey) + toParameterString(item)));
            }
        });

        return urls;
    }

    private static List<URL> appItemToUrls(ConfigItem item, String appKey) {
        List<URL> urls = new ArrayList<>();
        List<String> addresses = item.getAddresses();
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        if (addresses.size() == 0) {
            addresses.add(Constants.ANYHOST_VALUE);
        }
        for (String addr : addresses) {
            String urlStr = "override://" + addr + "/";
            List<String> services = item.getServices();
            if (services == null) {
                services = new ArrayList<>();
            }
            if (services.size() == 0) {
                services.add("*/*:*");
            }
            for (String s : services) {
                urls.add(URL.valueOf(urlStr + appendService(s) + toParameterString(item) + "&application=" + appKey));
            }
        }
        return urls;
    }

    private static String toParameterString(ConfigItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("category=");
        sb.append(Constants.DYNAMIC_CONFIGURATORS_CATEGORY);
        if (item.getSide() != null) {
            sb.append("&side=");
            sb.append(item.getSide());
        }
        ConfiguratorRule rules = item.getRules();
        if (rules == null || (rules.getThreadpool() == null && rules.getConfig() == null && rules.getCluster() == null)) {
            throw new IllegalStateException("Invalid configurator rules!");
        }
        if (rules.getThreadpool() != null) {
            rules.getThreadpool().forEach((k, v) -> {
                sb.append("&");
                sb.append(k);
                sb.append("=");
                sb.append(v);
            });
        }
        if (rules.getCluster() != null) {
            rules.getCluster().forEach((k, v) -> {
                sb.append("&");
                sb.append(k);
                sb.append("=");
                sb.append(v);
            });
        }
        if (rules.getConfig() != null) {
            rules.getConfig().forEach((k, v) -> {
                sb.append("&");
                sb.append(k);
                sb.append("=");
                sb.append(v);
            });
        }

        return sb.toString();
    }

    private static String appendService(String serviceKey) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(serviceKey)) {
            throw new IllegalStateException("service field in coniguration is null!");
        }
        String interfaceName = serviceKey;
        int i = interfaceName.indexOf("/");
        if (i > 0) {
            sb.append("group=");
            sb.append(interfaceName.substring(0, i));

            interfaceName = interfaceName.substring(i + 1);
        }
        int j = interfaceName.indexOf(":");
        if (j > 0) {
            sb.append("version=");
            sb.append(interfaceName.substring(j + 1));
            interfaceName = interfaceName.substring(0, j);
        }
        sb.insert(0, interfaceName + "?");

        return sb.toString();
    }

}
