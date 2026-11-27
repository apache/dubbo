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
package org.apache.dubbo.xds.traffic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.re2j.PatternSyntaxException;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction.HashPolicy;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.RegexMatchAndSubstitute;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.RegexMatcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XdsHashPolicyTest {
    @Test
    void resourceRefreshKeepsTheOriginalRequestHash() {
        long hash = Long.MIN_VALUE + 123;
        XdsRouteSelection selected = new XdsRouteSelection(
                XdsRingHashTest.snapshot(), null, "ring", Collections.emptyMap(), null, Collections.emptyMap(), hash);
        assertEquals(hash, selected.updated(XdsRingHashTest.snapshot()).getRequestHash());
    }

    @Test
    void xxh64MatchesGrpcJavaForByteTailsBlocksAsciiAndChannelIds() throws Exception {
        Class<?> type = Class.forName("io.grpc.xds.XxHash64");
        Field instance = type.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        Object oracle = instance.get(null);
        Method bytes = type.getDeclaredMethod("hashBytes", byte[].class);
        Method string = type.getDeclaredMethod("hashAsciiString", String.class);
        Method number = type.getDeclaredMethod("hashLong", long.class);
        bytes.setAccessible(true);
        string.setAccessible(true);
        number.setAccessible(true);
        assertEquals(0xef46db3751d8e999L, XdsHash64.bytes(new byte[0]));
        Random random = new Random(42);
        for (int length = 0; length <= 257; length++) {
            byte[] input = new byte[length];
            random.nextBytes(input);
            assertEquals(bytes.invoke(oracle, (Object) input), XdsHash64.bytes(input), "length=" + length);
        }
        for (String input : Arrays.asList(
                "", "alice", "/127.0.0.1:17070_0", "one,two", "雪", String.join("", Collections.nCopies(40, "ab")))) {
            assertEquals(string.invoke(oracle, input), XdsHash64.string(input));
        }
        for (long input : new long[] {0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE, 123456789}) {
            assertEquals(number.invoke(oracle, input), XdsHash64.number(input));
        }
    }

    @Test
    void headerOrderTerminalAndMissingHeadersFollowTheA42Combination() {
        HashPolicy first = header("x-first", false);
        HashPolicy second = header("x-second", true);
        HashPolicy last = header("x-last", false);
        Map<String, String> headers = new HashMap<>();
        headers.put("x-first", "first");
        headers.put("x-second", "second");
        headers.put("x-last", "last");
        long expected = Long.rotateLeft(XdsHash64.string("first"), 1) ^ XdsHash64.string("second");
        assertEquals(expected, new XdsHashPolicy(Arrays.asList(first, second, last)).hash(headers, 42));
        headers.remove("x-second");
        assertEquals(
                XdsHash64.string("first"), new XdsHashPolicy(Arrays.asList(first, second, last)).hash(headers, 42));
        headers.remove("x-first");
        assertEquals(XdsHash64.string("last"), new XdsHashPolicy(Arrays.asList(first, second, last)).hash(headers, 42));
    }

    @Test
    void regexRewriteNormalizesTheHeaderBeforeHashingAndRejectsInvalidRe2() {
        HashPolicy policy = header("X-User", true).toBuilder()
                .setHeader(HashPolicy.Header.newBuilder()
                        .setHeaderName("X-User")
                        .setRegexRewrite(RegexMatchAndSubstitute.newBuilder()
                                .setPattern(RegexMatcher.newBuilder().setRegex("user-[0-9]+"))
                                .setSubstitution("user")))
                .build();
        assertEquals(
                XdsHash64.string("user,user"),
                new XdsHashPolicy(Collections.singletonList(policy))
                        .hash(Collections.singletonMap("x-user", "user-1,user-2"), 1));
        HashPolicy invalid = policy.toBuilder()
                .setHeader(policy.getHeader().toBuilder()
                        .setRegexRewrite(policy.getHeader().getRegexRewrite().toBuilder()
                                .setPattern(RegexMatcher.newBuilder().setRegex("["))))
                .build();
        assertThrows(PatternSyntaxException.class, () -> new XdsHashPolicy(Collections.singletonList(invalid)));
    }

    @Test
    void channelIdIsStablePerReferenceAndBinaryHeadersDoNotContribute() {
        HashPolicy channel = HashPolicy.newBuilder()
                .setFilterState(HashPolicy.FilterState.newBuilder().setKey("io.grpc.channel_id"))
                .build();
        XdsHashPolicy policy = new XdsHashPolicy(Arrays.asList(header("secret-bin", true), channel));
        assertEquals(XdsHash64.number(42), policy.hash(Collections.singletonMap("secret-bin", "ignored"), 42));
        assertNotEquals(policy.hash(Collections.emptyMap(), 42), policy.hash(Collections.emptyMap(), 43));
        assertEquals(
                XdsHash64.string("application/grpc"),
                new XdsHashPolicy(Collections.singletonList(header("content-type", true)))
                        .hash(Collections.singletonMap("content-type", "application/grpc+proto"), 1));
    }

    @Test
    void unsupportedAndAbsentPoliciesUseRandomFallbackWithoutPretendingToHashCookiesOrSourceIp() {
        XdsHashPolicy policy = new XdsHashPolicy(Arrays.asList(
                HashPolicy.newBuilder()
                        .setCookie(HashPolicy.Cookie.newBuilder().setName("cookie"))
                        .build(),
                HashPolicy.newBuilder()
                        .setConnectionProperties(
                                HashPolicy.ConnectionProperties.newBuilder().setSourceIp(true))
                        .build(),
                HashPolicy.newBuilder()
                        .setQueryParameter(
                                HashPolicy.QueryParameter.newBuilder().setName("user"))
                        .build(),
                HashPolicy.newBuilder()
                        .setFilterState(HashPolicy.FilterState.newBuilder().setKey("unknown"))
                        .build(),
                header("missing", true)));
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) seen.add(policy.hash(Collections.singletonMap("cookie", "x"), 42));
        assertTrue(seen.size() > 95);
    }

    static HashPolicy header(String name, boolean terminal) {
        return HashPolicy.newBuilder()
                .setHeader(HashPolicy.Header.newBuilder().setHeaderName(name))
                .setTerminal(terminal)
                .build();
    }
}
