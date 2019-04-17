package com.alibaba.dubbo.common.utils;

import com.alibaba.dubbo.common.URL;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author cvictory ON 2019-04-17
 */
public class UrlUtils {

    public static URL parseURL(String address, Map<String, String> defaults) {
        return new URL(UrlUtils.parseURL(address, defaults));
    }

    public static List<URL> parseURLs(String address, Map<String, String> defaults) {
        return UrlUtils.parseURLs(address, defaults).stream().map(e -> new URL(e)).collect(Collectors.toList());
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
