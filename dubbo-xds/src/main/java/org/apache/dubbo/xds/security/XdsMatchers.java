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
import java.util.function.Predicate;

import com.google.re2j.Pattern;
import io.grpc.xds.shaded.io.envoyproxy.envoy.type.matcher.v3.StringMatcher;

/** RE2 matching prevents unbounded backtracking in control-plane regular expressions. */
public final class XdsMatchers {
    private XdsMatchers() {}

    public static Predicate<String> string(StringMatcher matcher) {
        SecurityFields.only(matcher, "exact", "prefix", "suffix", "contains", "safe_regex", "ignore_case");
        final boolean ignoreCase = matcher.getIgnoreCase();
        switch (matcher.getMatchPatternCase()) {
            case EXACT:
                return value -> value != null
                        && (ignoreCase ? value.equalsIgnoreCase(matcher.getExact()) : value.equals(matcher.getExact()));
            case PREFIX:
                return value -> value != null
                        && value.regionMatches(
                                ignoreCase,
                                0,
                                matcher.getPrefix(),
                                0,
                                matcher.getPrefix().length());
            case SUFFIX:
                return value -> value != null
                        && value.regionMatches(
                                ignoreCase,
                                value.length() - matcher.getSuffix().length(),
                                matcher.getSuffix(),
                                0,
                                matcher.getSuffix().length());
            case CONTAINS:
                String needle = ignoreCase ? matcher.getContains().toLowerCase(Locale.ROOT) : matcher.getContains();
                return value -> value != null && (ignoreCase ? value.toLowerCase(Locale.ROOT) : value).contains(needle);
            case SAFE_REGEX:
                SecurityFields.only(matcher.getSafeRegex(), "regex", "google_re2");
                SecurityFields.only(matcher.getSafeRegex().getGoogleRe2());
                if (ignoreCase) {
                    throw new IllegalArgumentException("ignore_case is unsupported with safe_regex");
                }
                Pattern expression = Pattern.compile(matcher.getSafeRegex().getRegex());
                return value -> value != null && expression.matcher(value).matches();
            default:
                throw new IllegalArgumentException("Unsupported xDS string matcher");
        }
    }
}
