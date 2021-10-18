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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLStrParser;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.url.component.ServiceConfigURL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CLASSIFIER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.ENABLED_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HOST_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PASSWORD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PORT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.REGISTRY_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.REMOVE_VALUE_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.USERNAME_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.OVERRIDE_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_TYPE_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTE_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_TYPE;

public class UrlUtils {

    /**
     * in the url string,mark the param begin
     */
    private final static String URL_PARAM_STARTING_SYMBOL = "?";

    public static URL parseURL(String address, Map<String, String> defaults) {
        if (StringUtils.isEmpty(address)) {
            throw new IllegalArgumentException("Address is not allowed to be empty, please re-enter.");
        }
        String url;
        if (address.contains("://") || address.contains(URL_PARAM_STARTING_SYMBOL)) {
            url = address;
        } else {
            String[] addresses = COMMA_SPLIT_PATTERN.split(address);
            url = addresses[0];
            if (addresses.length > 1) {
                StringBuilder backup = new StringBuilder();
                for (int i = 1; i < addresses.length; i++) {
                    if (i > 1) {
                        backup.append(',');
                    }
                    backup.append(addresses[i]);
                }
                url += URL_PARAM_STARTING_SYMBOL + RemotingConstants.BACKUP_KEY + "=" + backup.toString();
            }
        }
        String defaultProtocol = defaults == null ? null : defaults.get(PROTOCOL_KEY);
        if (StringUtils.isEmpty(defaultProtocol)) {
            defaultProtocol = DUBBO_PROTOCOL;
        }
        String defaultUsername = defaults == null ? null : defaults.get(USERNAME_KEY);
        String defaultPassword = defaults == null ? null : defaults.get(PASSWORD_KEY);
        int defaultPort = StringUtils.parseInteger(defaults == null ? null : defaults.get(PORT_KEY));
        String defaultPath = defaults == null ? null : defaults.get(PATH_KEY);
        Map<String, String> defaultParameters = defaults == null ? null : new HashMap<>(defaults);
        if (defaultParameters != null) {
            defaultParameters.remove(PROTOCOL_KEY);
            defaultParameters.remove(USERNAME_KEY);
            defaultParameters.remove(PASSWORD_KEY);
            defaultParameters.remove(HOST_KEY);
            defaultParameters.remove(PORT_KEY);
            defaultParameters.remove(PATH_KEY);
        }
        URL u = URL.cacheableValueOf(url);
        boolean changed = false;
        String protocol = u.getProtocol();
        String username = u.getUsername();
        String password = u.getPassword();
        String host = u.getHost();
        int port = u.getPort();
        String path = u.getPath();
        Map<String, String> parameters = new HashMap<>(u.getParameters());
        if (StringUtils.isEmpty(protocol)) {
            changed = true;
            protocol = defaultProtocol;
        }
        if (StringUtils.isEmpty(username) && StringUtils.isNotEmpty(defaultUsername)) {
            changed = true;
            username = defaultUsername;
        }
        if (StringUtils.isEmpty(password) && StringUtils.isNotEmpty(defaultPassword)) {
            changed = true;
            password = defaultPassword;
        }
        /*if (u.isAnyHost() || u.isLocalHost()) {
            changed = true;
            host = NetUtils.getLocalHost();
        }*/
        if (port <= 0) {
            if (defaultPort > 0) {
                changed = true;
                port = defaultPort;
            } else {
                changed = true;
                port = 9090;
            }
        }
        if (StringUtils.isEmpty(path)) {
            if (StringUtils.isNotEmpty(defaultPath)) {
                changed = true;
                path = defaultPath;
            }
        }
        if (defaultParameters != null && defaultParameters.size() > 0) {
            for (Map.Entry<String, String> entry : defaultParameters.entrySet()) {
                String key = entry.getKey();
                String defaultValue = entry.getValue();
                if (StringUtils.isNotEmpty(defaultValue)) {
                    String value = parameters.get(key);
                    if (StringUtils.isEmpty(value)) {
                        changed = true;
                        parameters.put(key, defaultValue);
                    }
                }
            }
        }
        if (changed) {
            u = new ServiceConfigURL(protocol, username, password, host, port, path, parameters);
        }
        return u;
    }

    public static List<URL> parseURLs(String address, Map<String, String> defaults) {
        if (StringUtils.isEmpty(address)) {
            throw new IllegalArgumentException("Address is not allowed to be empty, please re-enter.");
        }
        String[] addresses = REGISTRY_SPLIT_PATTERN.split(address);
        if (addresses == null || addresses.length == 0) {
            throw new IllegalArgumentException("Addresses is not allowed to be empty, please re-enter."); //here won't be empty
        }
        List<URL> registries = new ArrayList<URL>();
        for (String addr : addresses) {
            registries.add(parseURL(addr, defaults));
        }
        return registries;
    }

    public static Map<String, Map<String, String>> convertRegister(Map<String, Map<String, String>> register) {
        Map<String, Map<String, String>> newRegister = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
            String serviceName = entry.getKey();
            Map<String, String> serviceUrls = entry.getValue();
            if (StringUtils.isNotContains(serviceName, ':') && StringUtils.isNotContains(serviceName, '/')) {
                for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                    String serviceUrl = entry2.getKey();
                    String serviceQuery = entry2.getValue();
                    Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                    String group = params.get(GROUP_KEY);
                    String version = params.get(VERSION_KEY);
                    //params.remove("group");
                    //params.remove("version");
                    String name = serviceName;
                    if (StringUtils.isNotEmpty(group)) {
                        name = group + "/" + name;
                    }
                    if (StringUtils.isNotEmpty(version)) {
                        name = name + ":" + version;
                    }
                    Map<String, String> newUrls = newRegister.computeIfAbsent(name, k -> new HashMap<>());
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
            if (StringUtils.isNotContains(serviceName, ':') && StringUtils.isNotContains(serviceName, '/')) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String group = params.get(GROUP_KEY);
                String version = params.get(VERSION_KEY);
                //params.remove("group");
                //params.remove("version");
                String name = serviceName;
                if (StringUtils.isNotEmpty(group)) {
                    name = group + "/" + name;
                }
                if (StringUtils.isNotEmpty(version)) {
                    name = name + ":" + version;
                }
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        }
        return newSubscribe;
    }

    public static Map<String, Map<String, String>> revertRegister(Map<String, Map<String, String>> register) {
        Map<String, Map<String, String>> newRegister = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> entry : register.entrySet()) {
            String serviceName = entry.getKey();
            Map<String, String> serviceUrls = entry.getValue();
            if (StringUtils.isContains(serviceName, ':') && StringUtils.isContains(serviceName, '/')) {
                for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                    String serviceUrl = entry2.getKey();
                    String serviceQuery = entry2.getValue();
                    Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                    String name = serviceName;
                    int i = name.indexOf('/');
                    if (i >= 0) {
                        params.put(GROUP_KEY, name.substring(0, i));
                        name = name.substring(i + 1);
                    }
                    i = name.lastIndexOf(':');
                    if (i >= 0) {
                        params.put(VERSION_KEY, name.substring(i + 1));
                        name = name.substring(0, i);
                    }
                    Map<String, String> newUrls = newRegister.computeIfAbsent(name, k -> new HashMap<String, String>());
                    newUrls.put(serviceUrl, StringUtils.toQueryString(params));
                }
            } else {
                newRegister.put(serviceName, serviceUrls);
            }
        }
        return newRegister;
    }

    public static Map<String, String> revertSubscribe(Map<String, String> subscribe) {
        Map<String, String> newSubscribe = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : subscribe.entrySet()) {
            String serviceName = entry.getKey();
            String serviceQuery = entry.getValue();
            if (StringUtils.isContains(serviceName, ':') && StringUtils.isContains(serviceName, '/')) {
                Map<String, String> params = StringUtils.parseQueryString(serviceQuery);
                String name = serviceName;
                int i = name.indexOf('/');
                if (i >= 0) {
                    params.put(GROUP_KEY, name.substring(0, i));
                    name = name.substring(i + 1);
                }
                i = name.lastIndexOf(':');
                if (i >= 0) {
                    params.put(VERSION_KEY, name.substring(i + 1));
                    name = name.substring(0, i);
                }
                newSubscribe.put(name, StringUtils.toQueryString(params));
            } else {
                newSubscribe.put(serviceName, serviceQuery);
            }
        }
        return newSubscribe;
    }

    public static Map<String, Map<String, String>> revertNotify(Map<String, Map<String, String>> notify) {
        if (notify != null && notify.size() > 0) {
            Map<String, Map<String, String>> newNotify = new HashMap<>();
            for (Map.Entry<String, Map<String, String>> entry : notify.entrySet()) {
                String serviceName = entry.getKey();
                Map<String, String> serviceUrls = entry.getValue();
                if (StringUtils.isNotContains(serviceName, ':') && StringUtils.isNotContains(serviceName, '/')) {
                    if (CollectionUtils.isNotEmptyMap(serviceUrls)) {
                        for (Map.Entry<String, String> entry2 : serviceUrls.entrySet()) {
                            String url = entry2.getKey();
                            String query = entry2.getValue();
                            Map<String, String> params = StringUtils.parseQueryString(query);
                            String group = params.get(GROUP_KEY);
                            String version = params.get(VERSION_KEY);
                            // params.remove("group");
                            // params.remove("version");
                            String name = serviceName;
                            if (StringUtils.isNotEmpty(group)) {
                                name = group + "/" + name;
                            }
                            if (StringUtils.isNotEmpty(version)) {
                                name = name + ":" + version;
                            }
                            Map<String, String> newUrls = newNotify.computeIfAbsent(name, k -> new HashMap<>());
                            newUrls.put(url, StringUtils.toQueryString(params));
                        }
                    }
                } else {
                    newNotify.put(serviceName, serviceUrls);
                }
            }
            return newNotify;
        }
        return notify;
    }

    //compatible for dubbo-2.0.0
    public static List<String> revertForbid(List<String> forbid, Set<URL> subscribed) {
        if (CollectionUtils.isNotEmpty(forbid)) {
            List<String> newForbid = new ArrayList<>();
            for (String serviceName : forbid) {
                if (StringUtils.isNotContains(serviceName, ':') && StringUtils.isNotContains(serviceName, '/')) {
                    for (URL url : subscribed) {
                        if (serviceName.equals(url.getServiceInterface())) {
                            newForbid.add(url.getServiceKey());
                            break;
                        }
                    }
                } else {
                    newForbid.add(serviceName);
                }
            }
            return newForbid;
        }
        return forbid;
    }

    public static URL getEmptyUrl(String service, String category) {
        String group = null;
        String version = null;
        int i = service.indexOf('/');
        if (i > 0) {
            group = service.substring(0, i);
            service = service.substring(i + 1);
        }
        i = service.lastIndexOf(':');
        if (i > 0) {
            version = service.substring(i + 1);
            service = service.substring(0, i);
        }
        return URL.valueOf(EMPTY_PROTOCOL + "://0.0.0.0/" + service + URL_PARAM_STARTING_SYMBOL
            + CATEGORY_KEY + "=" + category
            + (group == null ? "" : "&" + GROUP_KEY + "=" + group)
            + (version == null ? "" : "&" + VERSION_KEY + "=" + version));
    }

    public static boolean isMatchCategory(String category, String categories) {
        if (categories == null || categories.length() == 0) {
            return DEFAULT_CATEGORY.equals(category);
        } else if (categories.contains(ANY_VALUE)) {
            return true;
        } else if (categories.contains(REMOVE_VALUE_PREFIX)) {
            return !categories.contains(REMOVE_VALUE_PREFIX + category);
        } else {
            return categories.contains(category);
        }
    }

    public static boolean isMatch(URL consumerUrl, URL providerUrl) {
        String consumerInterface = consumerUrl.getServiceInterface();
        String providerInterface = providerUrl.getServiceInterface();
        //FIXME accept providerUrl with '*' as interface name, after carefully thought about all possible scenarios I think it's ok to add this condition.
        if (!(ANY_VALUE.equals(consumerInterface)
            || ANY_VALUE.equals(providerInterface)
            || StringUtils.isEquals(consumerInterface, providerInterface))) {
            return false;
        }

        if (!isMatchCategory(providerUrl.getCategory(DEFAULT_CATEGORY),
            consumerUrl.getCategory(DEFAULT_CATEGORY))) {
            return false;
        }
        if (!providerUrl.getParameter(ENABLED_KEY, true)
            && !ANY_VALUE.equals(consumerUrl.getParameter(ENABLED_KEY))) {
            return false;
        }

        String consumerGroup = consumerUrl.getGroup();
        String consumerVersion = consumerUrl.getVersion();
        String consumerClassifier = consumerUrl.getParameter(CLASSIFIER_KEY, ANY_VALUE);

        String providerGroup = providerUrl.getGroup();
        String providerVersion = providerUrl.getVersion();
        String providerClassifier = providerUrl.getParameter(CLASSIFIER_KEY, ANY_VALUE);
        return (ANY_VALUE.equals(consumerGroup) || StringUtils.isEquals(consumerGroup, providerGroup) || StringUtils.isContains(consumerGroup, providerGroup))
            && (ANY_VALUE.equals(consumerVersion) || StringUtils.isEquals(consumerVersion, providerVersion))
            && (consumerClassifier == null || ANY_VALUE.equals(consumerClassifier) || StringUtils.isEquals(consumerClassifier, providerClassifier));
    }

    public static boolean isMatchGlobPattern(String pattern, String value, URL param) {
        if (param != null && pattern.startsWith("$")) {
            pattern = param.getRawParameter(pattern.substring(1));
        }
        return isMatchGlobPattern(pattern, value);
    }

    public static boolean isMatchGlobPattern(String pattern, String value) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (StringUtils.isEmpty(pattern) && StringUtils.isEmpty(value)) {
            return true;
        }
        if (StringUtils.isEmpty(pattern) || StringUtils.isEmpty(value)) {
            return false;
        }

        int i = pattern.lastIndexOf('*');
        // doesn't find "*"
        if (i == -1) {
            return value.equals(pattern);
        }
        // "*" is at the end
        else if (i == pattern.length() - 1) {
            return value.startsWith(pattern.substring(0, i));
        }
        // "*" is at the beginning
        else if (i == 0) {
            return value.endsWith(pattern.substring(i + 1));
        }
        // "*" is in the middle
        else {
            String prefix = pattern.substring(0, i);
            String suffix = pattern.substring(i + 1);
            return value.startsWith(prefix) && value.endsWith(suffix);
        }
    }

    public static boolean isServiceKeyMatch(URL pattern, URL value) {
        return pattern.getParameter(INTERFACE_KEY).equals(
            value.getParameter(INTERFACE_KEY))
            && isItemMatch(pattern.getGroup(),
            value.getGroup())
            && isItemMatch(pattern.getVersion(),
            value.getVersion());
    }

    public static List<URL> classifyUrls(List<URL> urls, Predicate<URL> predicate) {
        return urls.stream().filter(predicate).collect(Collectors.toList());
    }

    public static boolean isConfigurator(URL url) {
        return OVERRIDE_PROTOCOL.equals(url.getProtocol()) ||
            CONFIGURATORS_CATEGORY.equals(url.getCategory(DEFAULT_CATEGORY));
    }

    public static boolean isRoute(URL url) {
        return ROUTE_PROTOCOL.equals(url.getProtocol()) ||
            ROUTERS_CATEGORY.equals(url.getCategory(DEFAULT_CATEGORY));
    }

    public static boolean isProvider(URL url) {
        return !OVERRIDE_PROTOCOL.equals(url.getProtocol()) &&
            !ROUTE_PROTOCOL.equals(url.getProtocol()) &&
            PROVIDERS_CATEGORY.equals(url.getCategory(PROVIDERS_CATEGORY));
    }

    public static boolean isRegistry(URL url) {
        return REGISTRY_PROTOCOL.equals(url.getProtocol())
            || SERVICE_REGISTRY_PROTOCOL.equalsIgnoreCase(url.getProtocol())
            || (url.getProtocol() != null && url.getProtocol().endsWith("-registry-protocol"));
    }

    /**
     * The specified {@link URL} is service discovery registry type or not
     *
     * @param url the {@link URL} connects to the registry
     * @return If it is, return <code>true</code>, or <code>false</code>
     * @since 2.7.5
     */
    public static boolean hasServiceDiscoveryRegistryTypeKey(URL url) {
        return hasServiceDiscoveryRegistryTypeKey(url == null ? emptyMap() : url.getParameters());
    }

    public static boolean hasServiceDiscoveryRegistryProtocol(URL url) {
        return SERVICE_REGISTRY_PROTOCOL.equalsIgnoreCase(url.getProtocol());
    }

    public static boolean isServiceDiscoveryURL(URL url) {
        return hasServiceDiscoveryRegistryProtocol(url) || hasServiceDiscoveryRegistryTypeKey(url);
    }

    /**
     * The specified parameters of {@link URL} is service discovery registry type or not
     *
     * @param parameters the parameters of {@link URL} that connects to the registry
     * @return If it is, return <code>true</code>, or <code>false</code>
     * @since 2.7.5
     */
    public static boolean hasServiceDiscoveryRegistryTypeKey(Map<String, String> parameters) {
        if (CollectionUtils.isEmptyMap(parameters)) {
            return false;
        }
        return SERVICE_REGISTRY_TYPE.equals(parameters.get(REGISTRY_TYPE_KEY));
    }

    /**
     * Check if the given value matches the given pattern. The pattern supports wildcard "*".
     *
     * @param pattern pattern
     * @param value   value
     * @return true if match otherwise false
     */
    static boolean isItemMatch(String pattern, String value) {
        if (StringUtils.isEmpty(pattern)) {
            return value == null;
        } else {
            return "*".equals(pattern) || pattern.equals(value);
        }
    }

    /**
     * @param serviceKey, {group}/{interfaceName}:{version}
     * @return [group, interfaceName, version]
     */
    public static String[] parseServiceKey(String serviceKey) {
        String[] arr = new String[3];
        int i = serviceKey.indexOf('/');
        if (i > 0) {
            arr[0] = serviceKey.substring(0, i);
            serviceKey = serviceKey.substring(i + 1);
        }

        int j = serviceKey.indexOf(':');
        if (j > 0) {
            arr[2] = serviceKey.substring(j + 1);
            serviceKey = serviceKey.substring(0, j);
        }
        arr[1] = serviceKey;
        return arr;
    }

    /**
     * NOTICE: This method allocate too much objects, we can use {@link URLStrParser#parseDecodedStr(String)} instead.
     * <p>
     * Parse url string
     *
     * @param url URL string
     * @return URL instance
     * @see URL
     */
    public static URL valueOf(String url) {
        if (url == null || (url = url.trim()).length() == 0) {
            throw new IllegalArgumentException("url == null");
        }
        String protocol = null;
        String username = null;
        String password = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = null;
        int i = url.indexOf('?'); // separator between body and parameters
        if (i >= 0) {
            String[] parts = url.substring(i + 1).split("&");
            parameters = new HashMap<>();
            for (String part : parts) {
                part = part.trim();
                if (part.length() > 0) {
                    int j = part.indexOf('=');
                    if (j >= 0) {
                        String key = part.substring(0, j);
                        String value = part.substring(j + 1);
                        parameters.put(key, value);
                        // compatible with lower versions registering "default." keys
                        if (key.startsWith(DEFAULT_KEY_PREFIX)) {
                            parameters.putIfAbsent(key.substring(DEFAULT_KEY_PREFIX.length()), value);
                        }
                    } else {
                        parameters.put(part, part);
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i >= 0) {
            if (i == 0) {
                throw new IllegalStateException("url missing protocol: \"" + url + "\"");
            }
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else {
            // case: file:/path/to/file.txt
            i = url.indexOf(":/");
            if (i >= 0) {
                if (i == 0) {
                    throw new IllegalStateException("url missing protocol: \"" + url + "\"");
                }
                protocol = url.substring(0, i);
                url = url.substring(i + 1);
            }
        }

        i = url.indexOf('/');
        if (i >= 0) {
            path = url.substring(i + 1);
            url = url.substring(0, i);
        }
        i = url.lastIndexOf('@');
        if (i >= 0) {
            username = url.substring(0, i);
            int j = username.indexOf(':');
            if (j >= 0) {
                password = username.substring(j + 1);
                username = username.substring(0, j);
            }
            url = url.substring(i + 1);
        }
        i = url.lastIndexOf(':');
        if (i >= 0 && i < url.length() - 1) {
            if (url.lastIndexOf('%') > i) {
                // ipv6 address with scope id
                // e.g. fe80:0:0:0:894:aeec:f37d:23e1%en0
                // see https://howdoesinternetwork.com/2013/ipv6-zone-id
                // ignore
            } else {
                port = Integer.parseInt(url.substring(i + 1));
                url = url.substring(0, i);
            }
        }
        if (url.length() > 0) {
            host = url;
        }

        return new ServiceConfigURL(protocol, username, password, host, port, path, parameters);
    }

    public static boolean isConsumer(URL url) {
        return url.getProtocol().equalsIgnoreCase(CONSUMER) || url.getPort() == 0;
    }

}
