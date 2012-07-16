/**
 * Project: dubbo.registry.client-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-4-22
 * $Id: RouteUtils.java 181192 2012-06-21 05:05:47Z tony.chenl $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.registry.common.route;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.common.domain.Provider;
import com.alibaba.dubbo.registry.common.domain.Route;
import com.alibaba.dubbo.registry.common.domain.Override;

/**
 * RouteParser 路由规则解析。
 * 
 * @author ding.lid
 */
public class RouteUtils {
    
    public static boolean matchRoute(String consumerAddress, String consumerQueryUrl, Route route, Map<String, List<String>> clusters) {
        RouteRule rule = RouteRule.parseQuitely(route);
        Map<String, RouteRule.MatchPair> when = RouteRuleUtils.expandCondition(
                rule.getWhenCondition(), "consumer.cluster", "consumer.host", clusters);
        Map<String, String> consumerSample = ParseUtils.parseQuery("consumer.", consumerQueryUrl);
        
        final int index = consumerAddress.lastIndexOf(":");
        String consumerHost = null;
        if(index != -1){
            consumerHost = consumerAddress.substring(0, index);
        }
        else {
            consumerHost = consumerAddress;
        }
        consumerSample.put("consumer.host", consumerHost);
        
        return RouteRuleUtils.isMatchCondition(when, consumerSample, consumerSample);
    }
    
    public static Map<String, String> previewRoute(String serviceName, String consumerAddress, String queryUrl, Map<String, String> serviceUrls,
            Route route, Map<String, List<String>> clusters, List<Route> routed) {
        if(null == route) {
            throw new IllegalArgumentException("Route is null.");
        }
        List<Route> routes = new ArrayList<Route>();
        routes.add(route);
        return route(serviceName, consumerAddress, queryUrl, serviceUrls, routes, clusters, routed);
    }

    /**
     * @return Map<methodName, Route>
     */
    public static List<Route> findUsedRoute(String serviceName, String consumerAddress, String consumerQueryUrl,
            List<Route> routes, Map<String, List<String>> clusters) {
    	List<Route> routed = new ArrayList<Route>();
    	Map<String, String> urls = new HashMap<String, String>();
    	urls.put("dubbo://" + consumerAddress + "/" + serviceName, consumerQueryUrl);
    	RouteUtils.route(serviceName, consumerAddress, consumerQueryUrl, urls, routes, clusters, routed);
    	return routed;
    }
    
    public static List<Provider> route(String serviceName, String consumerAddress, String consumerQueryUrl, List<Provider> providers,
    		List<Override> overrides, List<Route> routes, Map<String, List<String>> clusters, List<Route> routed) {
    	if (providers == null) {
    		return null;
    	}
    	Map<String, String> urls = new HashMap<String, String>();
    	urls.put("consumer://" + consumerAddress + "/" + serviceName, consumerQueryUrl); // not empty dummy data
    	for (Provider provider : providers) {
    		if (com.alibaba.dubbo.governance.web.common.pulltool.Tool.isProviderEnabled(provider, overrides)) {
    			urls.put(provider.getUrl(), provider.getParameters());
    		}
    	}
    	urls = RouteUtils.route(serviceName, consumerAddress, consumerQueryUrl, urls, routes, clusters, routed);
    	List<Provider> result = new ArrayList<Provider>();
    	for (Provider provider : providers) {
    		if (urls.containsKey(provider.getUrl())) {
    			result.add(provider);
    		}
    	}
    	return result;
    }
    
    /**
     * @param serviceName 服务名，如{@code com.alibaba.morgan.MemberService}
     * @param consumerAddress 消费者地址，如{@code 192.168.1.3:54333}
     * @param consumerQueryUrl 订阅参数，如 <code>aplication=nasdaq&dubbo=2.0.3&methods=updateItems,validateNew&revision=1.7.0</code>
     * @param serviceUrls 提供者
     * @param routes 全部路由
     * @param clusters 全部的集群
     * @return 返回路由结果， Map<url-body, url-params>
     */
    // FIXME clusters和routes的合并，可以在clusters或routes变化时预先做
    // FIXME 从Util方法中分离出Cache的操作
    public static Map<String, String> route(String serviceName, String consumerAddress, String consumerQueryUrl, Map<String, String> serviceUrls,
            List<Route> routes, Map<String, List<String>> clusters, List<Route> routed) {
        if (serviceUrls == null || serviceUrls.size() == 0) {
            return serviceUrls;
        }
        if (routes == null || routes.isEmpty()) {
            return serviceUrls;
        }

        Map<Long, RouteRule> rules = route2RouteRule(routes, clusters);
        
        final Map<String, String> consumerSample = ParseUtils.parseQuery("consumer.", consumerQueryUrl);
        final int index = consumerAddress.lastIndexOf(":");
        final String consumerHost;
        if(consumerAddress != null && index != -1){
            consumerHost = consumerAddress.substring(0, index);
        }
        else {
            consumerHost = consumerAddress;
        }
        consumerSample.put("consumer.host", consumerHost);
        
        Map<String, Map<String, String>> url2ProviderSample = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, String> entry : serviceUrls.entrySet()) {
            URI uri;
            try {
                uri = new URI(entry.getKey());
            } catch (URISyntaxException e) {
                throw new IllegalStateException("fail to parse url(" + entry.getKey() + "):" + e.getMessage(), e);
            }
            Map<String, String> sample = new HashMap<String, String>();
            sample.putAll(ParseUtils.parseQuery("provider.", entry.getValue()));
            sample.put("provider.protocol", uri.getScheme());
            sample.put("provider.host", uri.getHost());
            sample.put("provider.port", String.valueOf(uri.getPort()));
            
            url2ProviderSample.put(entry.getKey(), sample);
        }
        
        
        Map<String, Set<String>> url2Methods = new HashMap<String, Set<String>>();
        
        // consumer可以通过consumer.methods Key指定需要的方法
        String methodsString = consumerSample.get("consumer.methods");
        String[] methods = methodsString == null || methodsString.length() == 0 ? new String[]{Route.ALL_METHOD} : methodsString.split(ParseUtils.METHOD_SPLIT);
        for (String method : methods) {
            consumerSample.put("method", method);
            // NOTE: 
            // <*方法>只配置 <no method key> 
            // method1方法匹配 <no method key> 和 <method = method1>, 此时要把<no method key>的Route的优先级降低即可
            if (routes != null && routes.size() > 0) {
                for (Route route : routes) {
                    if (isSerivceNameMatched(route.getService(), serviceName)) {
                        RouteRule rule = rules.get(route.getId());
                        // 当满足when条件时
                        if (rule != null && RouteRuleUtils.isMatchCondition(
                                rule.getWhenCondition(), consumerSample, consumerSample)) {
                        	if (routed != null && ! routed.contains(route)) {
                        		routed.add(route);
                        	}
                            Map<String, RouteRule.MatchPair> then = rule.getThenCondition();
                            if (then != null) {
                                Map<String, Map<String, String>> tmp = getUrlsMatchedCondition(then, consumerSample, url2ProviderSample);
                                // 如果规则的结果是空，则该规则无效，使用所有Provider
                                if (route.isForce() || ! tmp.isEmpty()){
                                    url2ProviderSample = tmp;
                                }
                            }
                        }
                    }
                }
            }
            for (String url : url2ProviderSample.keySet()) {
                Set<String> mts = url2Methods.get(url);
                if (mts == null) {
                    mts = new HashSet<String>();
                    url2Methods.put(url, mts);
                }
                mts.add(method);
            }
        } // end of for methods

        return appendMethodsToUrls(serviceUrls, url2Methods);
    }
    
    static Map<Long, RouteRule> route2RouteRule(List<Route> routes,
                                                Map<String, List<String>> clusters) {
        Map<Long, RouteRule> rules = new HashMap<Long, RouteRule>();
        // route -> RouteRule
        if (routes != null && routes.size() > 0) {
            for(Route route: routes) {
                rules.put(route.getId(), RouteRule.parseQuitely(route));
            }
        }
        // expand the cluster parameters into conditions of routerule
        if (clusters != null && clusters.size() > 0) {
            Map<Long, RouteRule> rrs = new HashMap<Long, RouteRule>();
            for (Map.Entry<Long, RouteRule> entry : rules.entrySet()) {
                RouteRule rr = entry.getValue();
    
                Map<String, RouteRule.MatchPair> when = RouteRuleUtils.expandCondition(
                        rr.getWhenCondition(), "consumer.cluster", "consumer.host", clusters);
                Map<String, RouteRule.MatchPair> then = RouteRuleUtils.expandCondition(
                        rr.getThenCondition(), "provider.cluster", "provider.host", clusters);
    
                rrs.put(entry.getKey(), RouteRule.createFromCondition(when, then));
            }
            rules = rrs;
        }
        return rules;
    }

    static Map<String, String> appendMethodsToUrls(Map<String, String> serviceUrls,
                                                Map<String, Set<String>> url2Methods) {
        // 为URL上加上方法参数
        Map<String, String> results = new HashMap<String, String>();
        for (Map.Entry<String, Set<String>> entry : url2Methods.entrySet()) {
            String url = entry.getKey();
            String query = serviceUrls.get(url);
            
            Set<String> methodNames = entry.getValue();
            if (methodNames != null && methodNames.size() > 0) {
                String ms = StringUtils.join(methodNames.toArray(new String[0]), ParseUtils.METHOD_SPLIT);
                query = ParseUtils.replaceParameter(query, "methods", ms);
            }
            results.put(url, query);
        }
        return results;
    }
    
    static Route getFirstRouteMatchedWhenConditionOfRule(String serviceName, Map<String, String> consumerSample, List<Route> routes, Map<Long, RouteRule> routeRuleMap) {
        if (serviceName == null || serviceName.length() == 0) {
            return null;
        }
        if (routes != null && routes.size() > 0) {
            for (Route route : routes) {
                if (isSerivceNameMatched(route.getService(), serviceName)) {
                    RouteRule rule = routeRuleMap.get(route.getId());
                    // 当满足when条件时
                    if (rule != null && RouteRuleUtils.isMatchCondition(
                            rule.getWhenCondition(), consumerSample, consumerSample)) {
                        return route; // 第一个满足即返回
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * 支持匹配的Service
     * 
     * @param servicePattern
     * @param serviceName
     */
    static boolean isSerivceNameMatched(String servicePattern, String serviceName) {
        final int pip = servicePattern.indexOf('/');
        final int pi = serviceName.indexOf('/');
        if(pip != -1) { // pattern有group
            if(pi == -1) return false; // servicename无group 
            
            String gp = servicePattern.substring(0, pip);
            servicePattern = servicePattern.substring(pip + 1);
            
            String g = serviceName.substring(0, pi);
            if(!gp.equals(g)) return false;
        }
        if(pi != -1) 
            serviceName = serviceName.substring(pi + 1);
        
        final int vip = servicePattern.lastIndexOf(':');
        final int vi = serviceName.lastIndexOf(':');
        if(vip != -1) { // pattern有group
            if(vi == -1) return false;
            
            String vp = servicePattern.substring(vip + 1);
            servicePattern = servicePattern.substring(0, vip);
            
            String v = serviceName.substring(vi + 1);
            if(!vp.equals(v)) return false;
        }
        if(vi != -1)
            serviceName = serviceName.substring(0, vi);
        
        return ParseUtils.isMatchGlobPattern(servicePattern, serviceName);
    }
    
    static Map<String, Map<String, String>> getUrlsMatchedCondition(Map<String, RouteRule.MatchPair> condition,
            Map<String, String> parameters, Map<String, Map<String, String>> url2Sample) {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> entry : url2Sample.entrySet()) {
            Map<String, String> sample = entry.getValue();
            
            Map<String, String> params = new HashMap<String, String>();
            params.putAll(sample);
            params.putAll(parameters);
            
            if (RouteRuleUtils.isMatchCondition(condition, params, sample)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
