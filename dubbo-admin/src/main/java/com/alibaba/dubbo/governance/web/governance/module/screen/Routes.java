/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.OwnerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.service.RouteService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Consumer;
import com.alibaba.dubbo.registry.common.domain.Provider;
import com.alibaba.dubbo.registry.common.domain.Route;
import com.alibaba.dubbo.registry.common.route.ParseUtils;
import com.alibaba.dubbo.registry.common.route.RouteRule;
import com.alibaba.dubbo.registry.common.route.RouteUtils;
import com.alibaba.dubbo.registry.common.util.Tool;

import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Providers.
 * URI: /services/$service/routes
 *
 * @author ding.lid
 * @author william.liangf
 * @author tony.chenl
 */
public class Routes extends Restful {

    private static final int MAX_RULE_LENGTH = 1000;
    static String[][] when_names = {
            {"method", "method", "unmethod"},
            {"consumer.application", "consumerApplication", "unconsumerApplication"},
            {"consumer.cluster", "consumerCluster", "unconsumerCluster"},
            {"consumer.host", "consumerHost", "unconsumerHost"},
            {"consumer.version", "consumerVersion", "unconsumerVersion"},
            {"consumer.group", "consumerGroup", "unconsumerGroup"},
    };
    static String[][] then_names = {
            {"provider.application", "providerApplication", "unproviderApplication"},
            {"provider.cluster", "providerCluster", "unproviderCluster"}, // 要校验Cluster是否存在
            {"provider.host", "providerHost", "unproviderHost"},
            {"provider.protocol", "providerProtocol", "unproviderProtocol"},
            {"provider.port", "providerPort", "unproviderPort"},
            {"provider.version", "providerVersion", "unproviderVersion"},
            {"provider.group", "providerGroup", "unproviderGroup"}
    };
    @Autowired
    private RouteService routeService;
    @Autowired
    private ProviderService providerService;
    @Autowired
    private ConsumerService consumerService;

    static void checkService(String service) {
        if (service.contains(",")) throw new IllegalStateException("service(" + service + ") contain illegale ','");

        String interfaceName = service;
        int gi = interfaceName.indexOf("/");
        if (gi != -1) interfaceName = interfaceName.substring(gi + 1);
        int vi = interfaceName.indexOf(':');
        if (vi != -1) interfaceName = interfaceName.substring(0, vi);

        if (interfaceName.indexOf('*') != -1 && interfaceName.indexOf('*') != interfaceName.length() - 1) {
            throw new IllegalStateException("service(" + service + ") only allow 1 *, and must be last char!");
        }
    }

    /**
     * 添加与服务相关的Owner
     *
     * @param usernames   用于添加的用户名
     * @param serviceName 不含通配符
     */
    public static void addOwnersOfService(Set<String> usernames, String serviceName,
                                          OwnerService ownerDAO) {
        List<String> serviceNamePatterns = ownerDAO.findAllServiceNames();
        for (String p : serviceNamePatterns) {
            if (ParseUtils.isMatchGlobPattern(p, serviceName)) {
                List<String> list = ownerDAO.findUsernamesByServiceName(p);
                usernames.addAll(list);
            }
        }
    }

    /**
     * 添加与服务模式相关的Owner
     *
     * @param usernames          用于添加的用户名
     * @param serviceNamePattern 服务模式，Glob模式
     */
    public static void addOwnersOfServicePattern(Set<String> usernames, String serviceNamePattern,
                                                 OwnerService ownerDAO) {
        List<String> serviceNamePatterns = ownerDAO.findAllServiceNames();
        for (String p : serviceNamePatterns) {
            if (ParseUtils.hasIntersection(p, serviceNamePattern)) {
                List<String> list = ownerDAO.findUsernamesByServiceName(p);
                usernames.addAll(list);
            }
        }
    }

    /**
     * 路由模块首页
     *
     * @param context
     */
    public void index(Map<String, Object> context) {
        String service = (String) context.get("service");
        String address = (String) context.get("address");
        address = Tool.getIP(address);
        List<Route> routes;
        if (service != null && service.length() > 0
                && address != null && address.length() > 0) {
            routes = routeService.findByServiceAndAddress(service, address);
        } else if (service != null && service.length() > 0) {
            routes = routeService.findByService(service);
        } else if (address != null && address.length() > 0) {
            routes = routeService.findByAddress(address);
        } else {
            routes = routeService.findAll();
        }
        context.put("routes", routes);
    }

    /**
     * 显示路由详细信息
     *
     * @param context
     */
    public void show(Map<String, Object> context) {
        try {
            Route route = routeService.findRoute(Long.parseLong((String) context.get("id")));

            if (route == null) {
                throw new IllegalArgumentException("The route is not existed.");
            }
            if (route.getService() != null && !route.getService().isEmpty()) {
                context.put("service", route.getService());
            }

            RouteRule routeRule = RouteRule.parse(route);

            @SuppressWarnings("unchecked")
            Map<String, RouteRule.MatchPair>[] paramArray = new Map[]{
                    routeRule.getWhenCondition(), routeRule.getThenCondition()};
            String[][][] namesArray = new String[][][]{when_names, then_names};

            for (int i = 0; i < paramArray.length; ++i) {
                Map<String, RouteRule.MatchPair> param = paramArray[i];
                String[][] names = namesArray[i];
                for (String[] name : names) {
                    RouteRule.MatchPair matchPair = param.get(name[0]);
                    if (matchPair == null) {
                        continue;
                    }

                    if (!matchPair.getMatches().isEmpty()) {
                        String m = RouteRule.join(matchPair.getMatches());
                        context.put(name[1], m);
                    }
                    if (!matchPair.getUnmatches().isEmpty()) {
                        String u = RouteRule.join(matchPair.getUnmatches());
                        context.put(name[2], u);
                    }
                }
            }
            context.put("route", route);
            context.put("methods", CollectionUtils.sort(new ArrayList<String>(providerService.findMethodsByService(route.getService()))));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * 载入新增路由页面
     *
     * @param context
     */
    public void add(Map<String, Object> context) {
        String service = (String) context.get("service");
        if (service != null && service.length() > 0 && !service.contains("*")) {
            context.put("service", service);
            context.put("methods", CollectionUtils.sort(new ArrayList<String>(providerService.findMethodsByService(service))));
        } else {
            List<String> serviceList = Tool.sortSimpleName(new ArrayList<String>(providerService.findServices()));
            context.put("serviceList", serviceList);
        }

        if (context.get("input") != null) context.put("input", context.get("input"));

    }

    /**
     * 载入修改路由页面
     *
     * @param context
     */
    public void edit(Map<String, Object> context) {
        add(context);
        show(context);
    }

    /**
     * 保存路由信息到数据库中
     *
     * @param context
     * @return
     */
    public boolean create(Map<String, Object> context) {
        String name = (String) context.get("name");
        String service = (String) context.get("service");
        if (StringUtils.isNotEmpty(service)
                && StringUtils.isNotEmpty(name)) {
            checkService(service);

            Map<String, String> when_name2valueList = new HashMap<String, String>();
            Map<String, String> notWhen_name2valueList = new HashMap<String, String>();
            for (String[] names : when_names) {
                when_name2valueList.put(names[0], (String) context.get(names[1]));
                notWhen_name2valueList.put(names[0], (String) context.get(names[2])); // value不为null的情况，这里处理，后面会保证
            }

            Map<String, String> then_name2valueList = new HashMap<String, String>();
            Map<String, String> notThen_name2valueList = new HashMap<String, String>();
            for (String[] names : then_names) {
                then_name2valueList.put(names[0], (String) context.get(names[1]));
                notThen_name2valueList.put(names[0], (String) context.get(names[2]));
            }

            RouteRule routeRule = RouteRule.createFromNameAndValueListString(
                    when_name2valueList, notWhen_name2valueList,
                    then_name2valueList, notThen_name2valueList);

            if (routeRule.getThenCondition().isEmpty()) {
                context.put("message", getMessage("Add route error! then is empty."));
                return false;
            }

            String matchRule = routeRule.getWhenConditionString();
            String filterRule = routeRule.getThenConditionString();

            //限制表达式的长度
            if (matchRule.length() > MAX_RULE_LENGTH) {
                context.put("message", getMessage("When rule is too long!"));
                return false;
            }
            if (filterRule.length() > MAX_RULE_LENGTH) {
                context.put("message", getMessage("Then rule is too long!"));
                return false;
            }

            Route route = new Route();
            route.setService(service);
            route.setName(name);
            route.setUsername((String) context.get("operator"));
            route.setOperator((String) context.get("operatorAddress"));
            route.setRule(routeRule.toString());
            if (StringUtils.isNotEmpty((String) context.get("priority"))) {
                route.setPriority(Integer.parseInt((String) context.get("priority")));
            }
            routeService.createRoute(route);

        }

        return true;
    }

    /**
     * 保存更新数据到数据库中
     *
     * @param context
     * @return
     */
    public boolean update(Map<String, Object> context) {
        String idStr = (String) context.get("id");
        if (idStr != null && idStr.length() > 0) {
            String[] blacks = (String[]) context.get("black");
            boolean black = false;
            if (blacks != null && blacks.length > 0) {
                black = true;
            }

            Route oldRoute = routeService.findRoute(Long.valueOf(idStr));
            if (null == oldRoute) {
                context.put("message", getMessage("NoSuchRecord"));
                return false;
            }
            //判断参数，拼凑rule
            if (StringUtils.isNotEmpty((String) context.get("name"))) {
                String service = oldRoute.getService();
                if (context.get("operator") == null) {
                    context.put("message", getMessage("HaveNoServicePrivilege", service));
                    return false;
                }

                Map<String, String> when_name2valueList = new HashMap<String, String>();
                Map<String, String> notWhen_name2valueList = new HashMap<String, String>();
                for (String[] names : when_names) {
                    when_name2valueList.put(names[0], (String) context.get(names[1]));
                    notWhen_name2valueList.put(names[0], (String) context.get(names[2])); // value不为null的情况，这里处理，后面会保证
                }

                Map<String, String> then_name2valueList = new HashMap<String, String>();
                Map<String, String> notThen_name2valueList = new HashMap<String, String>();
                for (String[] names : then_names) {
                    then_name2valueList.put(names[0], (String) context.get(names[1]));
                    notThen_name2valueList.put(names[0], (String) context.get(names[2]));
                }

                RouteRule routeRule = RouteRule.createFromNameAndValueListString(
                        when_name2valueList, notWhen_name2valueList,
                        then_name2valueList, notThen_name2valueList);

                RouteRule result = null;
                if (black) {
                    RouteRule.MatchPair matchPair = routeRule.getThenCondition().get("black");
                    Map<String, RouteRule.MatchPair> then = null;
                    if (null == matchPair) {
                        matchPair = new RouteRule.MatchPair();
                        then = new HashMap<String, RouteRule.MatchPair>();
                        then.put("black", matchPair);
                    } else {
                        matchPair.getMatches().clear();
                    }
                    matchPair.getMatches().add(String.valueOf(black));
                    result = RouteRule.copyWithReplace(routeRule, null, then);
                }

                if (result == null) {
                    result = routeRule;
                }

                if (result.getThenCondition().isEmpty()) {
                    context.put("message", getMessage("Update route error! then is empty."));
                    return false;
                }

                String matchRule = result.getWhenConditionString();
                String filterRule = result.getThenConditionString();

                //限制表达式的长度
                if (matchRule.length() > MAX_RULE_LENGTH) {
                    context.put("message", getMessage("When rule is too long!"));
                    return false;
                }
                if (filterRule.length() > MAX_RULE_LENGTH) {
                    context.put("message", getMessage("Then rule is too long!"));
                    return false;
                }

                int priority = 0;
                if (StringUtils.isNotEmpty((String) context.get("priority"))) {
                    priority = Integer.parseInt((String) context.get("priority"));
                }

                Route route = new Route();
                route.setRule(result.toString());
                route.setService(service);
                route.setPriority(priority);
                route.setName((String) context.get("name"));
                route.setUsername((String) context.get("operator"));
                route.setOperator((String) context.get("operatorAddress"));
                route.setId(Long.valueOf(idStr));
                route.setPriority(Integer.parseInt((String) context.get("priority")));
                route.setEnabled(oldRoute.isEnabled());
                routeService.updateRoute(route);

                Set<String> usernames = new HashSet<String>();
                usernames.add((String) context.get("operator"));
                usernames.add(route.getUsername());
                //RelateUserUtils.addOwnersOfService(usernames, route.getService(), ownerDAO);

                Map<String, Object> params = new HashMap<String, Object>();
                params.put("action", "update");
                params.put("route", route);

            } else {
                context.put("message", getMessage("MissRequestParameters", "name"));
            }
        } else {
            context.put("message", getMessage("MissRequestParameters", "id"));
        }

        return true;
    }

    /**
     * 删除指定ID的route规则
     *
     * @param ids
     * @return
     */
    public boolean delete(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            routeService.deleteRoute(id);
        }

        return true;
    }

    /**
     * 启用指定ID的route规则（可以批量处理）
     *
     * @param ids
     * @return
     */
    public boolean enable(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            routeService.enableRoute(id);
        }

        return true;
    }

    /**
     * 禁用指定ID的route规则（可以批量处理）
     *
     * @param ids
     * @return
     */
    public boolean disable(Long[] ids, Map<String, Object> context) {
        for (Long id : ids) {
            routeService.disableRoute(id);
        }

        return true;
    }

    /**
     * 选择消费者
     *
     * @param context
     */
    public void routeselect(Map<String, Object> context) {
        long rid = Long.valueOf((String) context.get("id"));
        context.put("id", rid);

        Route route = routeService.findRoute(rid);
        if (route == null) {
            throw new IllegalStateException("Route(id=" + rid + ") is not existed!");
        }

        context.put("route", route);
        // 获取数据
        List<Consumer> consumers = consumerService.findByService(route.getService());
        context.put("consumers", consumers);

        Map<String, Boolean> matchRoute = new HashMap<String, Boolean>();
        for (Consumer c : consumers) {
            matchRoute.put(c.getAddress(), RouteUtils.matchRoute(c.getAddress(), null, route, null));
        }
        context.put("matchRoute", matchRoute);
    }

    public void preview(Map<String, Object> context) throws Exception {
        String rid = (String) context.get("id");
        String consumerid = (String) context.get("cid");


        if (StringUtils.isEmpty(rid)) {
            context.put("message", getMessage("MissRequestParameters", "id"));
        }

        Map<String, String> serviceUrls = new HashMap<String, String>();
        Route route = routeService.findRoute(Long.valueOf(rid));
        if (null == route) {
            context.put("message", getMessage("NoSuchRecord"));
        }
        List<Provider> providers = providerService.findByService(route.getService());
        if (providers != null) {
            for (Provider p : providers) {
                serviceUrls.put(p.getUrl(), p.getParameters());
            }
        }
        if (StringUtils.isNotEmpty(consumerid)) {
            Consumer consumer = consumerService.findConsumer(Long.valueOf(consumerid));
            if (null == consumer) {
                context.put("message", getMessage("NoSuchRecord"));
            }
            Map<String, String> result = RouteUtils.previewRoute(consumer.getService(), consumer.getAddress(), consumer.getParameters(), serviceUrls,
                    route, null, null);
            context.put("route", route);
            context.put("consumer", consumer);
            context.put("result", result);
        } else {
            String address = (String) context.get("address");
            String service = (String) context.get("service");

            Map<String, String> result = RouteUtils.previewRoute(service, address, null, serviceUrls,
                    route, null, null);
            context.put("route", route);

            Consumer consumer = new Consumer();
            consumer.setService(service);
            consumer.setAddress(address);
            context.put("consumer", consumer);
            context.put("result", result);
        }

    }
}
