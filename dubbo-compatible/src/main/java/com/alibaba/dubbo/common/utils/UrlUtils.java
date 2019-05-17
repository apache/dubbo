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
package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.URL;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 2019-04-17
 */
@Deprecated
public class UrlUtils {

    public static URL parseURL(String address, Map<String, String> defaults) {
        return new URL(org.apache.dubbo.common.utils.UrlUtils.parseURL(address, defaults));
    }

    public static List<URL> parseURLs(String address, Map<String, String> defaults) {
        return org.apache.dubbo.common.utils.UrlUtils.parseURLs(address, defaults).stream().map(e -> new URL(e)).collect(Collectors.toList());
    }

    public static Map<String, Map<String, String>> convertRegister(Map<String, Map<String, String>> register) {
        return org.apache.dubbo.common.utils.UrlUtils.convertRegister(register);
    }

    public static Map<String, String> convertSubscribe(Map<String, String> subscribe) {
        return org.apache.dubbo.common.utils.UrlUtils.convertSubscribe(subscribe);
    }

    public static Map<String, Map<String, String>> revertRegister(Map<String, Map<String, String>> register) {
        return org.apache.dubbo.common.utils.UrlUtils.revertRegister(register);
    }

    public static Map<String, String> revertSubscribe(Map<String, String> subscribe) {
        return org.apache.dubbo.common.utils.UrlUtils.revertSubscribe(subscribe);
    }

    public static Map<String, Map<String, String>> revertNotify(Map<String, Map<String, String>> notify) {
        return org.apache.dubbo.common.utils.UrlUtils.revertNotify(notify);
    }

    //compatible for dubbo-2.0.0
    public static List<String> revertForbid(List<String> forbid, Set<URL> subscribed) {
        Set<org.apache.dubbo.common.URL> urls = subscribed.stream().map(e -> e.getOriginalURL()).collect(Collectors.toSet());
        return org.apache.dubbo.common.utils.UrlUtils.revertForbid(forbid, urls);
    }

    public static URL getEmptyUrl(String service, String category) {
        return new URL(org.apache.dubbo.common.utils.UrlUtils.getEmptyUrl(service, category));
    }

    public static boolean isMatchCategory(String category, String categories) {
        return org.apache.dubbo.common.utils.UrlUtils.isMatchCategory(category, categories);
    }

    public static boolean isMatch(URL consumerUrl, URL providerUrl) {
        return org.apache.dubbo.common.utils.UrlUtils.isMatch(consumerUrl.getOriginalURL(), providerUrl.getOriginalURL());
    }

    public static boolean isMatchGlobPattern(String pattern, String value, URL param) {
        return org.apache.dubbo.common.utils.UrlUtils.isMatchGlobPattern(pattern, value, param.getOriginalURL());
    }

    public static boolean isMatchGlobPattern(String pattern, String value) {
        return org.apache.dubbo.common.utils.UrlUtils.isMatchGlobPattern(pattern, value);
    }

    public static boolean isServiceKeyMatch(URL pattern, URL value) {
        return org.apache.dubbo.common.utils.UrlUtils.isServiceKeyMatch(pattern.getOriginalURL(), value.getOriginalURL());
    }


    public static boolean isConfigurator(URL url) {
        return org.apache.dubbo.common.utils.UrlUtils.isConfigurator(url.getOriginalURL());
    }

    public static boolean isRoute(URL url) {
        return org.apache.dubbo.common.utils.UrlUtils.isRoute(url.getOriginalURL());
    }

    public static boolean isProvider(URL url) {
        return org.apache.dubbo.common.utils.UrlUtils.isProvider(url.getOriginalURL());
    }

    public static int getHeartbeat(URL url) {
        return org.apache.dubbo.common.utils.UrlUtils.getHeartbeat(url.getOriginalURL());
    }

    public static int getIdleTimeout(URL url) {
        return org.apache.dubbo.common.utils.UrlUtils.getIdleTimeout(url.getOriginalURL());
    }
}
