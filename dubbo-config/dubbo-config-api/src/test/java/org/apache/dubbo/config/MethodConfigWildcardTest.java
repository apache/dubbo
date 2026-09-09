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
package org.apache.dubbo.config;

import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.config.api.DemoService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for wildcard method name support of {@link MethodConfig}, see the resolution rule in
 * {@link AbstractInterfaceConfig#resolveMethodConfigs(Class)}: a wildcard pattern ('*' / '?') is
 * expanded to every matched interface method, and an exact-named config always overrides a wildcard.
 */
class MethodConfigWildcardTest {

    private static final Set<String> DEMO_METHODS =
            new HashSet<>(Arrays.asList(ClassUtils.getMethodNames(DemoService.class)));

    /** Build a {@link MethodConfig} carrying only a name and a timeout used as an identity marker. */
    private static MethodConfig method(String name, int timeout) {
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName(name);
        methodConfig.setTimeout(timeout);
        return methodConfig;
    }

    private static Map<String, MethodConfig> resolve(List<MethodConfig> methods) {
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setMethods(methods);
        Map<String, MethodConfig> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, MethodConfig> entry :
                serviceConfig.resolveMethodConfigs(DemoService.class)) {
            resolved.put(entry.getKey(), entry.getValue());
        }
        return resolved;
    }

    @Test
    void testWildcardExpandsToMatchedMethods() {
        MethodConfig wildcard = method("get*", 100);
        Map<String, MethodConfig> resolved = resolve(Arrays.asList(wildcard));

        // DemoService declares getBox() and getUsers(), both start with "get".
        assertEquals(new HashSet<>(Arrays.asList("getBox", "getUsers")), resolved.keySet());
        assertSame(wildcard, resolved.get("getBox"));
        assertSame(wildcard, resolved.get("getUsers"));
    }

    @Test
    void testExactConfigTakesPrecedenceOverWildcard() {
        MethodConfig wildcard = method("get*", 100);
        MethodConfig exact = method("getBox", 200);
        Map<String, MethodConfig> resolved = resolve(Arrays.asList(wildcard, exact));

        assertEquals(new HashSet<>(Arrays.asList("getBox", "getUsers")), resolved.keySet());
        // getBox is governed by the exact config, getUsers falls back to the wildcard.
        assertSame(exact, resolved.get("getBox"));
        assertEquals(200, resolved.get("getBox").getTimeout());
        assertSame(wildcard, resolved.get("getUsers"));
        assertEquals(100, resolved.get("getUsers").getTimeout());
    }

    @Test
    void testOrderDoesNotAffectExactPrecedence() {
        // Declaring the exact config first must still make it win.
        MethodConfig exact = method("getBox", 200);
        MethodConfig wildcard = method("get*", 100);
        Map<String, MethodConfig> resolved = resolve(Arrays.asList(exact, wildcard));

        assertSame(exact, resolved.get("getBox"));
        assertSame(wildcard, resolved.get("getUsers"));
    }

    @Test
    void testWildcardWithoutMatchExpandsToNothing() {
        Map<String, MethodConfig> resolved = resolve(Arrays.asList(method("notExist*", 100)));
        assertTrue(resolved.isEmpty());
    }

    @Test
    void testAsteriskMatchesEveryMethod() {
        MethodConfig wildcard = method("*", 100);
        Map<String, MethodConfig> resolved = resolve(Arrays.asList(wildcard));

        assertEquals(DEMO_METHODS, resolved.keySet());
        resolved.values().forEach(config -> assertSame(wildcard, config));
    }

    @Test
    void testQuestionMarkMatchesExactlyOneCharacter() {
        // "ech?" -> echo (the '?' stands for the trailing 'o').
        Map<String, MethodConfig> matched = resolve(Arrays.asList(method("ech?", 100)));
        assertEquals(new HashSet<>(Arrays.asList("echo")), matched.keySet());

        // "echo?" requires one more character than "echo", so it does not match.
        Map<String, MethodConfig> unmatched = resolve(Arrays.asList(method("echo?", 100)));
        assertTrue(unmatched.isEmpty());
    }

    @Test
    void testFirstWildcardWinsAmongOverlappingWildcards() {
        MethodConfig first = method("get*", 100);
        MethodConfig second = method("*Box", 200);
        Map<String, MethodConfig> resolved = resolve(Arrays.asList(first, second));

        // Both patterns match getBox; the earlier wildcard keeps it.
        assertSame(first, resolved.get("getBox"));
    }

    @Test
    void testExactNameIsKeptAsIsForBackwardCompatibility() {
        MethodConfig exact = method("sayName", 100);
        Map<String, MethodConfig> resolved = resolve(Arrays.asList(exact));

        assertEquals(1, resolved.size());
        assertSame(exact, resolved.get("sayName"));
    }

    @Test
    void testNoMethodsResolvesToEmpty() {
        assertTrue(resolve(null).isEmpty());
    }

    @Test
    void testNullInterfaceResolvesToEmpty() {
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setMethods(Arrays.asList(method("get*", 100)));
        assertTrue(serviceConfig.resolveMethodConfigs(null).isEmpty());
    }

    @Test
    void testResolvedKeysAreAllRealInterfaceMethods() {
        Map<String, MethodConfig> resolved = resolve(Arrays.asList(method("*", 100)));
        Set<String> illegal = resolved.keySet()
                .stream()
                .filter(name -> !DEMO_METHODS.contains(name))
                .collect(Collectors.toSet());
        assertTrue(illegal.isEmpty(), "Resolved keys must be real interface methods: " + illegal);
    }
}
