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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfigItem;
import org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfiguratorConfig;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.RegistryConstants.APP_DYNAMIC_CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.rpc.cluster.Constants.OVERRIDE_PROVIDERS_KEY;
import static org.apache.dubbo.rpc.cluster.configurator.parser.model.ConfiguratorConfig.MATCH_CONDITION;

/**
 * Config parser
 */
public class ConfigParser {

    public static List<URL> parseConfigurators(String rawConfig) {
        // compatible url JsonArray, such as [ "override://xxx", "override://xxx" ]
        List<URL> compatibleUrls = parseJsonArray(rawConfig);
        if (CollectionUtils.isNotEmpty(compatibleUrls)) {
            return compatibleUrls;
        }

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

    private static List<URL> parseJsonArray(String rawConfig) {
        List<URL> urls = new ArrayList<>();
        try {
            List<String> list = JsonUtils.toJavaList(rawConfig, String.class);
            if (!CollectionUtils.isEmpty(list)) {
                list.forEach(u -> urls.add(URL.valueOf(u)));
            }
        } catch (Throwable t) {
            return null;
        }
        return urls;
    }

    private static ConfiguratorConfig parseObject(String rawConfig) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> map = yaml.load(rawConfig);
        return ConfiguratorConfig.parseFromMap(map);
    }

    private static List<URL> serviceItemToUrls(ConfigItem item, ConfiguratorConfig config) {
        List<URL> urls = new ArrayList<>();
        List<String> addresses = parseAddresses(item);

        addresses.forEach(addr -> {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("override://").append(addr).append('/');

            urlBuilder.append(appendService(config.getKey()));
            urlBuilder.append(toParameterString(item));

            parseEnabled(item, config, urlBuilder);

            urlBuilder.append("&configVersion=").append(config.getConfigVersion());

            List<String> apps = item.getApplications();
            if (CollectionUtils.isNotEmpty(apps)) {
                apps.forEach(app -> {
                    StringBuilder tmpUrlBuilder = new StringBuilder(urlBuilder);
                    urls.add(appendMatchCondition(URL.valueOf(tmpUrlBuilder.append("&application=").append(app).toString()), item));
                });
            } else {
                urls.add(appendMatchCondition(URL.valueOf(urlBuilder.toString()), item));
            }
        });

        return urls;
    }

    private static List<URL> appItemToUrls(ConfigItem item, ConfiguratorConfig config) {
        List<URL> urls = new ArrayList<>();
        List<String> addresses = parseAddresses(item);
        for (String addr : addresses) {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("override://").append(addr).append('/');
            List<String> services = item.getServices();
            if (services == null) {
                services = new ArrayList<>();
            }
            if (services.isEmpty()) {
                services.add("*");
            }
            for (String s : services) {
                StringBuilder tmpUrlBuilder = new StringBuilder(urlBuilder);
                tmpUrlBuilder.append(appendService(s));
                tmpUrlBuilder.append(toParameterString(item));

                tmpUrlBuilder.append("&application=").append(config.getKey());

                parseEnabled(item, config, tmpUrlBuilder);

                tmpUrlBuilder.append("&category=").append(APP_DYNAMIC_CONFIGURATORS_CATEGORY);
                tmpUrlBuilder.append("&configVersion=").append(config.getConfigVersion());

                urls.add(appendMatchCondition(URL.valueOf(tmpUrlBuilder.toString()), item));
            }
        }
        return urls;
    }

    private static String toParameterString(ConfigItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("category=");
        sb.append(DYNAMIC_CONFIGURATORS_CATEGORY);
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
            sb.append('&');
            sb.append(k);
            sb.append('=');
            sb.append(v);
        });

        if (CollectionUtils.isNotEmpty(item.getProviderAddresses())) {
            sb.append('&');
            sb.append(OVERRIDE_PROVIDERS_KEY);
            sb.append('=');
            sb.append(CollectionUtils.join(item.getProviderAddresses(), ","));
        } else if (PROVIDER.equals(item.getSide())) {
            sb.append('&');
            sb.append(OVERRIDE_PROVIDERS_KEY);
            sb.append('=');
            sb.append(CollectionUtils.join(parseAddresses(item), ","));
        }

        return sb.toString();
    }

    private static String appendService(String serviceKey) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(serviceKey)) {
            throw new IllegalStateException("service field in configuration is null.");
        }

        String interfaceName = serviceKey;
        int i = interfaceName.indexOf('/');
        if (i > 0) {
            sb.append("group=");
            sb.append(interfaceName, 0, i);
            sb.append('&');

            interfaceName = interfaceName.substring(i + 1);
        }
        int j = interfaceName.indexOf(':');
        if (j > 0) {
            sb.append("version=");
            sb.append(interfaceName.substring(j + 1));
            sb.append('&');
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
        if (addresses.isEmpty()) {
            addresses.add(ANYHOST_VALUE);
        }
        return addresses;
    }

    private static URL appendMatchCondition(URL url, ConfigItem item) {
        if (item.getMatch() != null) {
            url = url.putAttribute(MATCH_CONDITION, item.getMatch());
        }
        return url;
    }
}
