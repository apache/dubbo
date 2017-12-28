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
package com.alibaba.dubbo.registry.common.route;

import com.alibaba.dubbo.registry.common.domain.Route;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
// FIXME GhostMethod is Bug!! Should be fixed
public class RouteUtilsTest {

    private Map<String, String> serviceUrls = new HashMap<String, String>();
    private Map<String, String> serviceUrls_starMethods = new HashMap<String, String>();
    private Map<String, String> serviceUrls_ghostMethods = new HashMap<String, String>();
    private List<Route> routes = new ArrayList<Route>();
    private Map<String, List<String>> clusters;

    {
        serviceUrls.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=getPort,say&application=morgan");
        serviceUrls.put("dubbo://3.3.4.4:20881/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=getPort,say&application=udb");

        serviceUrls.put("dubbo://3.3.4.5:20882/hello.HelloService", "dubbo=2.0.0&version=2.0.0&revision=2.1.1&methods=getPort,say&application=pc2");
        serviceUrls.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=getPort,say&application=bops");
    }

    {
        serviceUrls_starMethods.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=*&application=morgan");
        serviceUrls_starMethods.put("dubbo://3.3.4.4:20881/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=*&application=udb");

        serviceUrls_starMethods.put("dubbo://3.3.4.5:20882/hello.HelloService", "dubbo=2.0.0&version=2.0.0&revision=2.1.1&methods=*&application=pc2");
        serviceUrls_starMethods.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=*&application=bops");
    }

    {
        serviceUrls_ghostMethods.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=getPort,say,ghostMethod&application=morgan");
        serviceUrls_ghostMethods.put("dubbo://3.3.4.4:20881/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=getPort,say,ghostMethod&application=udb");

        serviceUrls_ghostMethods.put("dubbo://3.3.4.5:20882/hello.HelloService", "dubbo=2.0.0&version=2.0.0&revision=2.1.1&methods=getPort,say,ghostMethod&application=pc2");
        serviceUrls_ghostMethods.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=getPort,say,ghostMethod&application=bops");
    }

    {
        Route r1 = new Route();
        r1.setId(1L);
        r1.setPriority(3);
        r1.setService("hello.HelloService");
        r1.setMatchRule("consumer.host = 1.1.2.2");
        r1.setFilterRule("provider.version = $consumer.version");
        routes.add(r1);

        Route r2 = new Route();
        r2.setId(2L);
        r2.setPriority(2);
        r2.setService("hello.HelloService");
        r2.setMatchRule("consumer.host = 1.1.2.2");
        r2.setFilterRule("provider.version = $consumer.version & provider.group = $consumer.group");
        routes.add(r2);

        Route r3 = new Route();
        r3.setId(2L);
        r3.setPriority(2);
        r3.setService("hello.HelloService");
        r3.setMatchRule("consumer.host = 1.1.2.2");
        r3.setFilterRule("provider.version = $consumer.version & provider.group = $consumer.group");
        routes.add(r3);
    }

    {
        clusters = new HashMap<String, List<String>>();
        List<String> list1 = new ArrayList<String>();
        list1.add("7.7.7.7");
        list1.add("8.8.8.8");
        clusters.put("cluster1", list1);
    }

    @Test
    public void test_matchRoute() throws Exception {
        Route route = new Route();
        route.setId(1L);
        route.setPriority(3);
        route.setMatchRule("consumer.host = 1.1.2.2");
        route.setFilterRule("xxx = yyy");
        routes.add(route);

        {
            assertTrue(RouteUtils.matchRoute("1.1.2.2:20880", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", route, clusters));

            assertFalse(RouteUtils.matchRoute("9.9.9.9", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", route, clusters));

            route.setMatchRule("consumer.host = 1.1.2.2 & consumer.application = kylin");
            assertTrue(RouteUtils.matchRoute("1.1.2.2", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", route, clusters));

            route.setMatchRule("consumer.host = 1.1.2.2 & consumer.application = notExstied");
            assertFalse(RouteUtils.matchRoute("1.1.2.2", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say&application=kylin", route, clusters));
        }

        {
            route.setMatchRule("consumer.cluster = cluster1");

            assertTrue(RouteUtils.matchRoute("7.7.7.7:20880", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", route, clusters));

            assertFalse(RouteUtils.matchRoute("9.9.9.9", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", route, clusters));

            route.setMatchRule("consumer.cluster = cluster1 & consumer.application = kylin");
            assertTrue(RouteUtils.matchRoute("7.7.7.7", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", route, clusters));

            route.setMatchRule("consumer.cluster = cluster1 & consumer.application = notExstied");
            assertFalse(RouteUtils.matchRoute("7.7.7.7", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say&application=kylin", route, clusters));
        }
    }

    @Test
    public void test_previewRoute() throws Exception {
        Route route = new Route();
        route.setId(1L);
        route.setService("hello.HelloService");
        route.setMatchRule("consumer.host=1.1.2.2,2.2.2.3");
        route.setFilterRule("provider.host=3.3.4.4&provider.application=morgan");

        {
            // no methods
            Map<String, String> preview = RouteUtils.previewRoute("hello.HelloService", "1.1.2.2:20880", "application=morgan", serviceUrls, route, clusters, null);
            Map<String, String> expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=*&application=morgan");
            assertEquals(expected, preview);

            // 2 methods
            preview = RouteUtils.previewRoute("hello.HelloService", "1.1.2.2", "application=morgan&methods=getPort,say", serviceUrls, route, clusters, null);
            expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=getPort,say&application=morgan");
            assertEquals(expected, preview);

            // ghost methods
            preview = RouteUtils.previewRoute("hello.HelloService", "1.1.2.2", "application=morgan&methods=getPort,say,ghostMethod", serviceUrls, route, clusters, null);
            expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=getPort,say,ghostMethod&application=morgan");
            assertEquals(expected, preview);

            // no route
            preview = RouteUtils.previewRoute("hello.HelloService", "1.2.3.4", "application=morgan", serviceUrls, route, clusters, null);
            assertEquals(serviceUrls_starMethods, preview);

            // no route, 2 methods
            preview = RouteUtils.previewRoute("hello.HelloService", "1.2.3.4", "application=morgan&methods=getPort,say", serviceUrls, route, clusters, null);
            assertEquals(serviceUrls, preview);

            // no route, ghost methods
            preview = RouteUtils.previewRoute("hello.HelloService", "1.2.3.4", "application=morgan&methods=getPort,say,ghostMethod", serviceUrls, route, clusters, null);
            assertEquals(serviceUrls_ghostMethods, preview);
        }

        // with cluster
        {
            route.setMatchRule("consumer.cluster = cluster1");

            // no method
            Map<String, String> preview = RouteUtils.previewRoute("hello.HelloService", "7.7.7.7:20880", "application=morgan", serviceUrls, route, clusters, null);
            Map<String, String> expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=*&application=morgan");
            assertEquals(expected, preview);

            // 2 methods
            preview = RouteUtils.previewRoute("hello.HelloService", "7.7.7.7", "application=morgan&methods=getPort,say", serviceUrls, route, clusters, null);
            expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=getPort,say&application=morgan");
            assertEquals(expected, preview);

            // ghost method
            preview = RouteUtils.previewRoute("hello.HelloService", "7.7.7.7", "application=morgan&methods=getPort,say,ghostMethod", serviceUrls, route, clusters, null);
            expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.4:20880/hello.HelloService", "dubbo=2.0.0&version=1.0.0&revision=1.1.1&methods=getPort,say,ghostMethod&application=morgan");
            assertEquals(expected, preview);

            // no route, no methods
            preview = RouteUtils.previewRoute("hello.HelloService", "1.2.3.4", "application=morgan", serviceUrls, route, clusters, null);
            assertEquals(serviceUrls_starMethods, preview);

            // no route, 2 methods
            preview = RouteUtils.previewRoute("hello.HelloService", "1.2.3.4", "application=morgan&methods=getPort,say", serviceUrls, route, clusters, null);
            assertEquals(serviceUrls, preview);

            preview = RouteUtils.previewRoute("hello.HelloService", "1.2.3.4", "application=morgan&methods=getPort,say,ghostMethod", serviceUrls, route, clusters, null);
            assertEquals(serviceUrls_ghostMethods, preview);
        }
    }

    @Test
    public void testRoute() throws Exception {
        {
            // no method
            Map<String, String> result = RouteUtils.route("hello.HelloService:1.0.0", "1.1.2.2:20880", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", serviceUrls, routes, clusters, null);
            Map<String, String> expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=*&application=bops");
            assertEquals(expected, result);

            // 2 methods
            result = RouteUtils.route("cn/hello.HelloService", "1.1.2.2", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say&application=kylin", serviceUrls, routes, clusters, null);
            expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=getPort,say&application=bops");
            assertEquals(expected, result);

            // ghost method
            result = RouteUtils.route("cn/hello.HelloService:2.0.0", "1.1.2.2", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say,ghostMethod&application=kylin", serviceUrls, routes, clusters, null);
            expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=getPort,say,ghostMethod&application=bops");
            assertEquals(expected, result);

            // no route, no method
            result = RouteUtils.route("hello.HelloService", "1.2.3.4", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", serviceUrls, routes, clusters, null);
            assertEquals(serviceUrls_starMethods, result);

            // no route, 2 methods
            result = RouteUtils.route("hello.HelloService", "1.2.3.4", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say&application=kylin", serviceUrls, routes, clusters, null);
            assertEquals(serviceUrls, result);

            // no route, ghost method
            result = RouteUtils.route("hello.HelloService", "1.2.3.4", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say,ghostMethod&application=kylin", serviceUrls, routes, clusters, null);
            assertEquals(serviceUrls_ghostMethods, result);
        }

        // with cluster
        {
            routes.get(0).setMatchRule("consumer.cluster = cluster1");

            // no method
            Map<String, String> result = RouteUtils.route("hello.HelloService", "7.7.7.7:20880", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", serviceUrls, routes, clusters, null);
            Map<String, String> expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=*&application=bops");
            assertEquals(expected, result);

            // 2 methods
            result = RouteUtils.route("hello.HelloService", "7.7.7.7", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say&application=kylin", serviceUrls, routes, clusters, null);
            expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=getPort,say&application=bops");
            assertEquals(expected, result);

            // ghost method
            result = RouteUtils.route("hello.HelloService", "7.7.7.7", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say,ghostMethod&application=kylin", serviceUrls, routes, clusters, null);
            expected = new HashMap<String, String>();
            expected.put("dubbo://3.3.4.6:20883/hello.HelloService", "dubbo=2.0.0&version=3.0.0&revision=3.1.1&methods=getPort,say,ghostMethod&application=bops");
            assertEquals(expected, result);

            // no route, no method
            result = RouteUtils.route("hello.HelloService", "1.2.3.4", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&application=kylin", serviceUrls, routes, clusters, null);
            assertEquals(serviceUrls_starMethods, result);

            // no route, 2 methods
            result = RouteUtils.route("hello.HelloService", "1.2.3.4", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say&application=kylin", serviceUrls, routes, clusters, null);
            assertEquals(serviceUrls, result);

            // no route, ghost method
            result = RouteUtils.route("hello.HelloService", "1.2.3.4", "dubbo=2.0.0&version=3.0.0&revision=3.0.0&methods=getPort,say,ghostMethod&application=kylin", serviceUrls, routes, clusters, null);
            assertEquals(serviceUrls_ghostMethods, result);
        }
    }

    @Test
    public void test_isSerivceNameMatched() throws Exception {
        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.MemberService", "com.alibaba.morgan.MemberService:1.0.0"));
        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.MemberService", "cn/com.alibaba.morgan.MemberService:1.0.0"));
        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.MemberService", "cn/com.alibaba.morgan.MemberService"));
        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.MemberService", "com.alibaba.morgan.MemberService"));

        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.Member*", "com.alibaba.morgan.MemberService:1.0.0"));
        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.Member*", "cn/com.alibaba.morgan.MemberService:1.0.0"));
        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.Member*", "cn/com.alibaba.morgan.MemberService"));
        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.Member*", "com.alibaba.morgan.MemberService"));

        assertFalse(RouteUtils.isSerivceNameMatched("cn/com.alibaba.morgan.Member*", "com.alibaba.morgan.MemberService:1.0.0"));
        assertTrue(RouteUtils.isSerivceNameMatched("cn/com.alibaba.morgan.MemberService", "cn/com.alibaba.morgan.MemberService:1.0.0"));
        assertTrue(RouteUtils.isSerivceNameMatched("cn/com.alibaba.morgan.Member*", "cn/com.alibaba.morgan.MemberService"));
        assertFalse(RouteUtils.isSerivceNameMatched("cn/com.alibaba.morgan.Member*", "intl/com.alibaba.morgan.MemberService"));
        assertFalse(RouteUtils.isSerivceNameMatched("cn/com.alibaba.morgan.Member*", "com.alibaba.morgan.MemberService"));

        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.Member*:1.0.0", "com.alibaba.morgan.MemberService:1.0.0"));
        assertTrue(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.MemberService:1.0.0", "cn/com.alibaba.morgan.MemberService:1.0.0"));
        assertFalse(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.MemberService:1.0.0", "cn/com.alibaba.morgan.MemberService:2.0.0"));
        assertFalse(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.Member*:1.0.0", "cn/com.alibaba.morgan.MemberService"));
        assertFalse(RouteUtils.isSerivceNameMatched("com.alibaba.morgan.Member*:1.0.0", "com.alibaba.morgan.MemberService"));
    }
}

