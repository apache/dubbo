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
package org.apache.dubbo.xds.directory;

import org.apache.dubbo.xds.resource.common.ThreadSafeRandom;
import org.apache.dubbo.xds.resource.matcher.FractionMatcher;
import org.apache.dubbo.xds.resource.matcher.HeaderMatcher;
import org.apache.dubbo.xds.resource.matcher.PathMatcher;
import org.apache.dubbo.xds.resource.route.RouteMatch;
import org.apache.dubbo.xds.resource.route.VirtualHost;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import com.google.common.base.Joiner;
import io.grpc.Metadata;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utilities for performing virtual host domain name matching and route matching.
 */
public final class RoutingUtils {
    // Prevent instantiation.
    private RoutingUtils() {}

    /**
     * Returns the {@link VirtualHost} with the best match domain for the given hostname.
     */
    @Nullable
    static VirtualHost findVirtualHostForHostName(List<VirtualHost> virtualHosts, String hostName) {
        // Domain search order:
        //  1. Exact domain names: ``www.foo.com``.
        //  2. Suffix domain wildcards: ``*.foo.com`` or ``*-bar.foo.com``.
        //  3. Prefix domain wildcards: ``foo.*`` or ``foo-*``.
        //  4. Special wildcard ``*`` matching any domain.
        //
        //  The longest wildcards match first.
        //  Assuming only a single virtual host in the entire route configuration can match
        //  on ``*`` and a domain must be unique across all virtual hosts.
        int matchingLen = -1; // longest length of wildcard pattern that matches host name
        boolean exactMatchFound = false; // true if a virtual host with exactly matched domain found
        VirtualHost targetVirtualHost = null; // target VirtualHost with longest matched domain
        for (VirtualHost vHost : virtualHosts) {
            for (String domain : vHost.getDomains()) {
                boolean selected = false;
                if (matchHostName(hostName, domain)) { // matching
                    if (!domain.contains("*")) { // exact matching
                        exactMatchFound = true;
                        targetVirtualHost = vHost;
                        break;
                    } else if (domain.length() > matchingLen) { // longer matching pattern
                        selected = true;
                    } else if (domain.length() == matchingLen && domain.startsWith("*")) { // suffix matching
                        selected = true;
                    }
                }
                if (selected) {
                    matchingLen = domain.length();
                    targetVirtualHost = vHost;
                }
            }
            if (exactMatchFound) {
                break;
            }
        }
        return targetVirtualHost;
    }

    /**
     * Returns {@code true} iff {@code hostName} matches the domain name {@code pattern} with
     * case-insensitive.
     *
     * <p>Wildcard pattern rules:
     * <ol>
     * <li>A single asterisk (*) matches any domain.</li>
     * <li>Asterisk (*) is only permitted in the left-most or the right-most part of the pattern,
     *     but not both.</li>
     * </ol>
     */
    private static boolean matchHostName(String hostName, String pattern) {
        checkArgument(
                hostName.length() != 0 && !hostName.startsWith(".") && !hostName.endsWith("."), "Invalid host name");
        checkArgument(
                pattern.length() != 0 && !pattern.startsWith(".") && !pattern.endsWith("."),
                "Invalid pattern/domain name");

        hostName = hostName.toLowerCase(Locale.US);
        pattern = pattern.toLowerCase(Locale.US);
        // hostName and pattern are now in lower case -- domain names are case-insensitive.

        if (!pattern.contains("*")) {
            // Not a wildcard pattern -- hostName and pattern must match exactly.
            return hostName.equals(pattern);
        }
        // Wildcard pattern

        if (pattern.length() == 1) {
            return true;
        }

        int index = pattern.indexOf('*');

        // At most one asterisk (*) is allowed.
        if (pattern.indexOf('*', index + 1) != -1) {
            return false;
        }

        // Asterisk can only match prefix or suffix.
        if (index != 0 && index != pattern.length() - 1) {
            return false;
        }

        // HostName must be at least as long as the pattern because asterisk has to
        // match one or more characters.
        if (hostName.length() < pattern.length()) {
            return false;
        }

        if (index == 0 && hostName.endsWith(pattern.substring(1))) {
            // Prefix matching fails.
            return true;
        }

        // Pattern matches hostname if suffix matching succeeds.
        return index == pattern.length() - 1 && hostName.startsWith(pattern.substring(0, pattern.length() - 1));
    }

    /**
     * Returns {@code true} iff the given {@link RouteMatch} matches the RPC's full method name and
     * headers.
     */
    static boolean matchRoute(RouteMatch routeMatch, String fullMethodName, Metadata headers, ThreadSafeRandom random) {
        if (!matchPath(routeMatch.getPathMatcher(), fullMethodName)) {
            return false;
        }
        for (HeaderMatcher headerMatcher : routeMatch.getHeaderMatchers()) {
            if (!headerMatcher.matches(getHeaderValue(headers, headerMatcher.name()))) {
                return false;
            }
        }
        FractionMatcher fraction = routeMatch.getFractionMatcher();
        return fraction == null || random.nextInt(fraction.getDenominator()) < fraction.getNumerator();
    }

    private static boolean matchPath(PathMatcher pathMatcher, String fullMethodName) {
        if (pathMatcher.getPath() != null) {
            return pathMatcher.isCaseSensitive()
                    ? pathMatcher.getPath().equals(fullMethodName)
                    : pathMatcher.getPath().equalsIgnoreCase(fullMethodName);
        } else if (pathMatcher.getPrefix() != null) {
            return pathMatcher.isCaseSensitive()
                    ? fullMethodName.startsWith(pathMatcher.getPrefix())
                    : fullMethodName
                            .toLowerCase(Locale.US)
                            .startsWith(pathMatcher.getPrefix().toLowerCase(Locale.US));
        }
        return pathMatcher.getRegEx().matches(fullMethodName);
    }

    @Nullable
    private static String getHeaderValue(Metadata headers, String headerName) {
        if (headerName.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
            return null;
        }
        if (headerName.equals("content-type")) {
            return "application/grpc";
        }
        Metadata.Key<String> key;
        try {
            key = Metadata.Key.of(headerName, Metadata.ASCII_STRING_MARSHALLER);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Iterable<String> values = headers.getAll(key);
        return values == null ? null : Joiner.on(",").join(values);
    }
}
