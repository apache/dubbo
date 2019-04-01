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
package org.apache.dubbo.rpc.cluster.configurator.parser;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfigItem;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfiguratorConfig;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Config parser
 */
public class ConfigParser {

    public static List<URL> parseConfigurators(String rawConfig) {
        List<URL> urls = new ArrayList<>();
        ConfiguratorConfig configuratorConfig = parseObject(rawConfig);

        String scope = configuratorConfig.getScope();
        List<ConfigItem> items = configuratorConfig.getConfigs();

        if (ConfiguratorConfig.SCOPE_APPLICATION.equals(scope)) {
            items.forEach(item -> urls.addAll(appItemToUrls(item, configuratorConfig)));
        } else {
            // service scope by default.
            items.forEach(item -> urls.addAll(serviceItemToUrls(item, configuratorConfig)));
        }
        return urls;
    }

    private static <T> T parseObject(String rawConfig) {
        Constructor constructor = new Constructor(ConfiguratorConfig.class);
        TypeDescription itemDescription = new TypeDescription(ConfiguratorConfig.class);
        itemDescription.addPropertyParameters("items", ConfigItem.class);
        constructor.addTypeDescription(itemDescription);

        Yaml yaml = new Yaml(constructor);
        return yaml.load(rawConfig);
    }

    private static List<URL> serviceItemToUrls(ConfigItem item, ConfiguratorConfig config) {
        List<URL> urls = new ArrayList<>();
        List<String> addresses = parseAddresses(item);

        addresses.forEach(addr -> {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("override://").append(addr).append("/");

            urlBuilder.append(appendService(config.getKey()));
            urlBuilder.append(toParameterString(item));

            parseEnabled(item, config, urlBuilder);

            urlBuilder.append("&category=").append(Constants.DYNAMIC_CONFIGURATORS_CATEGORY);
            urlBuilder.append("&configVersion=").append(config.getConfigVersion());

            List<String> apps = item.getApplications();
            if (apps != null && apps.size() > 0) {
                apps.forEach(app -> {
                    urls.add(URL.valueOf(urlBuilder.append("&application=").append(app).toString()));
                });
            } else {
                urls.add(URL.valueOf(urlBuilder.toString()));
            }
        });

        return urls;
    }

    private static List<URL> appItemToUrls(ConfigItem item, ConfiguratorConfig config) {
        List<URL> urls = new ArrayList<>();
        List<String> addresses = parseAddresses(item);
        for (String addr : addresses) {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("override://").append(addr).append("/");
            List<String> services = item.getServices();
            if (services == null) {
                services = new ArrayList<>();
            }
            if (services.size() == 0) {
                services.add("*");
            }
            for (String s : services) {
                urlBuilder.append(appendService(s));
                urlBuilder.append(toParameterString(item));

                urlBuilder.append("&application=").append(config.getKey());

                parseEnabled(item, config, urlBuilder);

                urlBuilder.append("&category=").append(Constants.APP_DYNAMIC_CONFIGURATORS_CATEGORY);
                urlBuilder.append("&configVersion=").append(config.getConfigVersion());

                urls.add(URL.valueOf(urlBuilder.toString()));
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
        Map<String, String> parameters = item.getParameters();
        if (CollectionUtils.isEmptyMap(parameters)) {
            throw new IllegalStateException("Invalid configurator rule, please specify at least one parameter " +
                    "you want to change in the rule.");
        }

        parameters.forEach((k, v) -> {
            sb.append("&");
            sb.append(k);
            sb.append("=");
            sb.append(v);
        });

        if (CollectionUtils.isNotEmpty(item.getProviderAddresses())) {
            sb.append("&");
            sb.append(Constants.OVERRIDE_PROVIDERS_KEY);
            sb.append("=");
            sb.append(CollectionUtils.join(item.getProviderAddresses(), ","));
        }

        return sb.toString();
    }

    private static String appendService(String serviceKey) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(serviceKey)) {
            throw new IllegalStateException("service field in configuration is null.");
        }

        String interfaceName = serviceKey;
        int i = interfaceName.indexOf("/");
        if (i > 0) {
            sb.append("group=");
            sb.append(interfaceName, 0, i);
            sb.append("&");

            interfaceName = interfaceName.substring(i + 1);
        }
        int j = interfaceName.indexOf(":");
        if (j > 0) {
            sb.append("version=");
            sb.append(interfaceName.substring(j + 1));
            sb.append("&");
            interfaceName = interfaceName.substring(0, j);
        }
        sb.insert(0, interfaceName + "?");

        return sb.toString();
    }

    private static void parseEnabled(ConfigItem item, ConfiguratorConfig config, StringBuilder urlBuilder) {
        urlBuilder.append("&enabled=");
        if (item.getType() == null || ConfigItem.GENERAL_TYPE.equals(item.getType())) {
            urlBuilder.append(config.getEnabled());
        } else {
            urlBuilder.append(item.getEnabled());
        }
    }

    private static List<String> parseAddresses(ConfigItem item) {
        List<String> addresses = item.getAddresses();
        if (addresses == null) {
            addresses = new ArrayList<>();
        }
        if (addresses.size() == 0) {
            addresses.add(Constants.ANYHOST_VALUE);
        }
        return addresses;
    }
}
