/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry;


/**
 * @author ding.lid
 */
public class RegistryTestSupport {
    /*public static <T> void assertEqualsIgnoreOrde(Collection<T> expected, Collection<T> actual) {
        Set<T> expectedSet;
        if (expected instanceof Set) {
            expectedSet = (Set<T>) expected;
        } else {
            expectedSet = new HashSet<T>(expected);
        }

        Set<T> actualSet;
        if (actual instanceof Set) {
            actualSet = (Set<T>) actual;
        } else {
            actualSet = new HashSet<T>(actual);
        }

        Assert.assertEquals(expectedSet, actualSet);
    }

    public static final String              member_service_name = "com.alibaba.morgan.MemberService";

    public static final Map<String, String> member_urls1;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("remote://10.20.130.230:9090/memberService", "version=1.0.0");

        member_urls1 = Collections.unmodifiableMap(m);
    }

    public static final Map<String, String> member_urls2;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("remote://11.11.11.11:9090/memberService", "version=1.0.0");
        m.put("remote://22.22.22.22:9090/memberService", "version=1.0.0");
        m.put("remote://33.33.33.33:9090/memberService", "version=1.0.0");

        member_urls2 = Collections.unmodifiableMap(m);
    }

    public static final Map<String, String> member_urls3;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("remote://44.44.44.44:9090/memberService", "version=1.0.0");
        member_urls3 = Collections.unmodifiableMap(m);
    }

    public static final String              xxx_service_name    = "com.alibaba.xxx.XxxService";

    public static final Map<String, String> xxx_urls1;
    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("remote://11.11.11.11:9090/xxxService", "version=1.0.0");
        m.put("remote://22.22.22.22:9090/xxxService", "version=1.0.0");

        xxx_urls1 = Collections.unmodifiableMap(m);
    }

    public static void registerCheck(AbstractRegistry registry) {
        {
            List<URL> registeredList = registry
                    .getRegistered("com.alibaba.morgan.MemberService");
            assertEquals(0, registeredList.size());

            Map<String, Map<String, String>> registeredMap = registry.getRegistered();
            assertEquals(0, registeredMap.size());
        }

        {
            for (Map.Entry<String, String> entry : member_urls1.entrySet()) {
                registry.register(member_service_name, entry.getKey(), entry.getValue());
            }

            Map<String, String> registered = registry.getRegistered(member_service_name);
            assertEquals(1, registered.size());
            assertNotSame(member_urls1, registered);
            assertEquals(member_urls1, registered);

            Map<String, Map<String, String>> registeredMap = registry.getRegistered();
            assertEquals(1, registeredMap.size());
            assertTrue(registeredMap.containsKey(member_service_name));

            registered = registeredMap.get(member_service_name);
            assertEquals(1, registered.size());
            assertNotSame(member_urls1, registered);
            assertEquals(member_urls1, registered);
        }

        {
            registry.register(member_service_name, member_urls2);

            Map<String, String> registered = registry.getRegistered(member_service_name);

            final Map<String, String> urls = new HashMap<String, String>();
            urls.putAll(member_urls1);
            urls.putAll(member_urls2);

            assertEquals(urls, registered);

            Map<String, Map<String, String>> registeredMap = registry.getRegistered();
            assertEquals(1, registeredMap.size());
            assertTrue(registeredMap.containsKey(member_service_name));

            registered = registeredMap.get(member_service_name);
            assertEquals(urls, registered);
        }

        {
            Map<String, Map<String, String>> services = new HashMap<String, Map<String, String>>();
            services.put(member_service_name, member_urls3);
            services.put(xxx_service_name, xxx_urls1);

            registry.register(services);

            final Map<String, String> urls = new HashMap<String, String>();
            urls.putAll(member_urls1);
            urls.putAll(member_urls2);
            urls.putAll(member_urls3);
            {
                Map<String, String> registered = registry.getRegistered(member_service_name);
                assertEquals(urls, registered);

                registered = registry.getRegistered(xxx_service_name);
                assertEquals(xxx_urls1, registered);
            }

            {
                Map<String, Map<String, String>> registeredMap = registry.getRegistered();
                assertEquals(2, registeredMap.size());
                assertTrue(registeredMap.containsKey(member_service_name));
                assertTrue(registeredMap.containsKey(xxx_service_name));

                Map<String, String> registered = registeredMap.get(member_service_name);
                assertEquals(urls, registered);

                registered = registeredMap.get(xxx_service_name);
                assertEquals(xxx_urls1, registered);
            }
        }
    }

    public static void subscribeCheck(AbstractRegistry registry, NotificationListener mockNotifyListener) {
        String subscribed = registry.getSubscribed("com.alibaba.morgan.MemberService");
        assertNull(subscribed);

        Map<String, String> subscribedMap = registry.getSubscribed();
        assertEquals(0, subscribedMap.size());

        // query允许为null
        registry.subscribe("com.alibaba.xxx.XxxService", (String) null, mockNotifyListener);

        subscribed = registry.getSubscribed("com.alibaba.xxx.XxxService");
        assertEquals("", subscribed);

        subscribedMap = registry.getSubscribed();
        assertEquals(1, subscribedMap.size());
        assertTrue(subscribedMap.containsKey("com.alibaba.xxx.XxxService"));
        assertEquals("", subscribedMap.get("com.alibaba.xxx.XxxService"));

        registry.subscribe("com.alibaba.empty.EmptyService", "", mockNotifyListener);

        // query允许为空
        subscribed = registry.getSubscribed("com.alibaba.empty.EmptyService");
        assertEquals("", subscribed);

        subscribedMap = registry.getSubscribed();
        assertEquals(2, subscribedMap.size());
        assertTrue(subscribedMap.containsKey("com.alibaba.empty.EmptyService"));
        assertEquals("", subscribedMap.get("com.alibaba.empty.EmptyService"));

        // 多项值的Query
        registry.subscribe("com.alibaba.morgan.MemberService", "dog=bad,cat=god",
                mockNotifyListener);

        subscribed = registry.getSubscribed("com.alibaba.morgan.MemberService");
        assertEquals("dog=bad,cat=god", subscribed);

        subscribedMap = registry.getSubscribed();
        assertEquals(3, subscribedMap.size());
        String s = subscribedMap.get("com.alibaba.morgan.MemberService");
        assertEquals("dog=bad,cat=god", s);

        registry.subscribe("com.alibaba.complex.ComplexService",
                "version=1.0.0&application=kylin&methods=findPerson,findVAccount",
                mockNotifyListener);

        subscribedMap = registry.getSubscribed();
        assertEquals(4, subscribedMap.size());
        String q = subscribedMap.get("com.alibaba.complex.ComplexService");
        assertEquals("version=1.0.0&application=kylin&methods=findPerson,findVAccount", q);
    }*/
    
    public void testDummy() {
    }
}