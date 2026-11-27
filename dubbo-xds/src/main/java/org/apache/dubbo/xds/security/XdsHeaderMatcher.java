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
package org.apache.dubbo.xds.security;

import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;

/** Matching is shared by routing and request authorization against the wire header projection. */
public final class XdsHeaderMatcher {
    private XdsHeaderMatcher() {}

    public static Predicate<Map<String, String>> compile(HeaderMatcher matcher) {
        SecurityFields.only(
                matcher,
                "name",
                "exact_match",
                "safe_regex_match",
                "range_match",
                "present_match",
                "prefix_match",
                "suffix_match",
                "contains_match",
                "string_match",
                "invert_match",
                "treat_missing_header_as_empty");
        String name = matcher.getName().toLowerCase(Locale.ROOT);
        if ("host".equals(name)) {
            name = ":authority";
        }
        final String headerName = name;
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Empty RBAC header name");
        }
        Predicate<String> match;
        boolean presence = false;
        switch (matcher.getHeaderMatchSpecifierCase()) {
            case EXACT_MATCH:
                match = XdsMatchers.string(StringMatcher.newBuilder()
                        .setExact(matcher.getExactMatch())
                        .build());
                break;
            case PREFIX_MATCH:
                match = XdsMatchers.string(StringMatcher.newBuilder()
                        .setPrefix(matcher.getPrefixMatch())
                        .build());
                break;
            case SUFFIX_MATCH:
                match = XdsMatchers.string(StringMatcher.newBuilder()
                        .setSuffix(matcher.getSuffixMatch())
                        .build());
                break;
            case CONTAINS_MATCH:
                match = XdsMatchers.string(StringMatcher.newBuilder()
                        .setContains(matcher.getContainsMatch())
                        .build());
                break;
            case SAFE_REGEX_MATCH:
                match = XdsMatchers.string(StringMatcher.newBuilder()
                        .setSafeRegex(matcher.getSafeRegexMatch())
                        .build());
                break;
            case STRING_MATCH:
                match = XdsMatchers.string(matcher.getStringMatch());
                break;
            case PRESENT_MATCH:
                presence = true;
                match = value -> (value != null) == matcher.getPresentMatch();
                break;
            case RANGE_MATCH:
                SecurityFields.only(matcher.getRangeMatch(), "start", "end");
                long start = matcher.getRangeMatch().getStart();
                long end = matcher.getRangeMatch().getEnd();
                if (start >= end) {
                    throw new IllegalArgumentException("Invalid RBAC header range");
                }
                match = value -> {
                    try {
                        long number = Long.parseLong(value);
                        return number >= start && number < end;
                    } catch (NumberFormatException error) {
                        return false;
                    }
                };
                break;
            default:
                throw new IllegalArgumentException("Unsupported RBAC header matcher");
        }
        final boolean presentMatcher = presence;
        return headers -> {
            String value = headers.get(headerName);
            if (value == null && !presentMatcher) {
                if (!matcher.getTreatMissingHeaderAsEmpty()) {
                    return false;
                }
                value = "";
            }
            return match.test(value) != matcher.getInvertMatch();
        };
    }
}
