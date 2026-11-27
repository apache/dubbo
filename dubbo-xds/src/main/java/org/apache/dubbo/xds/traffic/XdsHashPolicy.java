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

import org.apache.dubbo.xds.security.SecurityFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.google.re2j.Pattern;
import io.grpc.xds.shaded.io.envoyproxy.envoy.config.route.v3.RouteAction.HashPolicy;

/** gRFC A42 route hashing. Unsupported policy kinds contribute no hash. */
final class XdsHashPolicy {
    static final String REFERENCE_ID_ATTRIBUTE = "dubbo.xds.hash-reference-id";
    private final List<Rule> rules = new ArrayList<>();

    XdsHashPolicy(List<HashPolicy> policies) {
        for (HashPolicy policy : policies) {
            switch (policy.getPolicySpecifierCase()) {
                case HEADER:
                    SecurityFields.only(policy.getHeader(), "header_name", "regex_rewrite");
                    String name = policy.getHeader().getHeaderName().toLowerCase(Locale.ROOT);
                    if (name.isEmpty()) throw new IllegalArgumentException("Empty xDS hash header name");
                    Pattern regex = null;
                    String substitution = null;
                    if (policy.getHeader().hasRegexRewrite()) {
                        SecurityFields.only(policy.getHeader().getRegexRewrite(), "pattern", "substitution");
                        if (policy.getHeader().getRegexRewrite().hasPattern()) {
                            SecurityFields.only(
                                    policy.getHeader().getRegexRewrite().getPattern(), "regex", "google_re2");
                            regex = Pattern.compile(policy.getHeader()
                                    .getRegexRewrite()
                                    .getPattern()
                                    .getRegex());
                            substitution = policy.getHeader().getRegexRewrite().getSubstitution();
                        }
                    }
                    rules.add(new Rule(name, regex, substitution, policy.getTerminal()));
                    break;
                case FILTER_STATE:
                    if ("io.grpc.channel_id".equals(policy.getFilterState().getKey())) {
                        rules.add(new Rule(null, null, null, policy.getTerminal()));
                    }
                    break;
                default:
                    // Cookies, source IP and query parameters do not have native gRPC request semantics.
            }
        }
    }

    long hash(Map<String, String> headers, long referenceId) {
        Long hash = null;
        for (Rule rule : rules) {
            Long next = null;
            if (rule.header == null) next = XdsHash64.number(referenceId);
            else if (!rule.header.endsWith("-bin")) {
                String value = "content-type".equals(rule.header) ? "application/grpc" : headers.get(rule.header);
                if (value != null) {
                    if (rule.regex != null) value = rule.regex.matcher(value).replaceAll(rule.substitution);
                    next = XdsHash64.string(value);
                }
            }
            if (next != null) hash = hash == null ? next : Long.rotateLeft(hash, 1) ^ next;
            if (rule.terminal && hash != null) break;
        }
        return hash == null ? ThreadLocalRandom.current().nextLong() : hash;
    }

    private static final class Rule {
        final String header;
        final Pattern regex;
        final String substitution;
        final boolean terminal;

        Rule(String header, Pattern regex, String substitution, boolean terminal) {
            this.header = header;
            this.regex = regex;
            this.substitution = substitution;
            this.terminal = terminal;
        }
    }
}
