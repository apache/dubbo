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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.cors.CorsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.utils.StringUtils.EMPTY_STRING_ARRAY;

public class CorsMeta {

    private final String[] allowedOrigins;
    private final Pattern[] allowedOriginsPatterns;
    private final String[] allowedMethods;
    private final String[] allowedHeaders;
    private final String[] exposedHeaders;
    private final Boolean allowCredentials;
    private final Long maxAge;

    private CorsMeta(
            String[] allowedOrigins,
            Pattern[] allowedOriginsPatterns,
            String[] allowedMethods,
            String[] allowedHeaders,
            String[] exposedHeaders,
            Boolean allowCredentials,
            Long maxAge) {
        this.allowedOrigins = allowedOrigins;
        this.allowedOriginsPatterns = allowedOriginsPatterns;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.exposedHeaders = exposedHeaders;
        this.allowCredentials = allowCredentials;
        this.maxAge = maxAge;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public Pattern[] getAllowedOriginsPatterns() {
        return allowedOriginsPatterns;
    }

    public String[] getAllowedMethods() {
        return allowedMethods;
    }

    public String[] getAllowedHeaders() {
        return allowedHeaders;
    }

    public String[] getExposedHeaders() {
        return exposedHeaders;
    }

    public Boolean getAllowCredentials() {
        return allowCredentials;
    }

    public Long getMaxAge() {
        return maxAge;
    }

    public boolean isEmpty() {
        return allowedOrigins.length == 0
                && allowedMethods.length == 0
                && allowedHeaders.length == 0
                && exposedHeaders.length == 0
                && allowCredentials == null
                && maxAge == null;
    }

    public CorsMeta applyDefault() {
        String[] allowedOriginArray = null;
        Pattern[] allowedOriginPatternArray = null;
        if (this.allowedOrigins.length == 0) {
            allowedOriginArray = new String[] {ANY_VALUE};
            allowedOriginPatternArray = new Pattern[] {null};
        }

        String[] allowedMethodArray = null;
        if (this.allowedMethods.length == 0) {
            allowedMethodArray =
                    new String[] {HttpMethods.GET.name(), HttpMethods.HEAD.name(), HttpMethods.POST.name()};
        }

        String[] allowedHeaderArray = null;
        if (this.allowedHeaders.length == 0) {
            allowedHeaderArray = new String[] {ANY_VALUE};
        }

        Long maxAgeValue = null;
        if (this.maxAge == null) {
            maxAgeValue = 1800L;
        }

        if (allowedOriginArray == null
                && allowedMethodArray == null
                && allowedHeaderArray == null
                && maxAgeValue == null) {
            return this;
        }

        return new CorsMeta(
                allowedOriginArray == null ? this.allowedOrigins : allowedOriginArray,
                allowedOriginPatternArray == null ? this.allowedOriginsPatterns : allowedOriginPatternArray,
                allowedMethodArray == null ? this.allowedMethods : allowedMethodArray,
                allowedHeaderArray == null ? this.allowedHeaders : allowedHeaderArray,
                exposedHeaders,
                allowCredentials,
                maxAgeValue);
    }

    public CorsMeta combine(CorsMeta other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        return new CorsMeta(
                combine(allowedOrigins, other.allowedOrigins),
                merge(allowedOriginsPatterns, other.allowedOriginsPatterns).toArray(new Pattern[0]),
                combine(allowedMethods, other.allowedMethods),
                combine(allowedHeaders, other.allowedHeaders),
                combine(exposedHeaders, other.exposedHeaders),
                other.allowCredentials == null ? allowCredentials : other.allowCredentials,
                other.maxAge == null ? maxAge : other.maxAge);
    }

    /**
     * Merge two arrays of CORS config values, with the other array having higher priority.
     */
    private static String[] combine(String[] source, String[] other) {
        if (other.length == 0) {
            return source;
        }
        if (source.length == 0 || source[0].equals(ANY_VALUE) || other[0].equals(ANY_VALUE)) {
            return other;
        }
        return merge(source, other).toArray(EMPTY_STRING_ARRAY);
    }

    private static <T> Set<T> merge(T[] source, T[] other) {
        int size = source.length + other.length;
        if (size == 0) {
            return Collections.emptySet();
        }
        Set<T> merged = CollectionUtils.newLinkedHashSet(size);
        Collections.addAll(merged, source);
        Collections.addAll(merged, other);
        return merged;
    }

    @Override
    public String toString() {
        return "CorsMeta{"
                + "allowedOrigins=" + Arrays.toString(allowedOrigins)
                + ", allowedOriginsPatterns=" + Arrays.toString(allowedOriginsPatterns)
                + ", allowedMethods=" + Arrays.toString(allowedMethods)
                + ", allowedHeaders=" + Arrays.toString(allowedHeaders)
                + ", exposedHeaders=" + Arrays.toString(exposedHeaders)
                + ", allowCredentials=" + allowCredentials
                + ", maxAge=" + maxAge
                + '}';
    }

    public static final class Builder {

        private static final Pattern PORTS_PATTERN = Pattern.compile("(.*):\\[(\\*|\\d+(,\\d+)*)]");

        private final Set<String> allowedOrigins = new LinkedHashSet<>();
        private final Set<String> allowedMethods = new LinkedHashSet<>();
        private final Set<String> allowedHeaders = new LinkedHashSet<>();
        private final Set<String> exposedHeaders = new LinkedHashSet<>();
        private Boolean allowCredentials;
        private Long maxAge;

        public Builder allowedOrigins(String... origins) {
            addValues(allowedOrigins, CorsUtils::formatOrigin, origins);
            return this;
        }

        public Builder allowedMethods(String... methods) {
            addValues(allowedMethods, v -> v.trim().toUpperCase(), methods);
            return this;
        }

        public Builder allowedHeaders(String... headers) {
            addValues(allowedHeaders, String::trim, headers);
            return this;
        }

        public Builder exposedHeaders(String... headers) {
            addValues(exposedHeaders, String::trim, headers);
            return this;
        }

        private static void addValues(Set<String> set, Function<String, String> fn, String... values) {
            if (values == null || set.contains(ANY_VALUE)) {
                return;
            }
            for (String value : values) {
                if (StringUtils.isNotEmpty(value)) {
                    value = fn.apply(value);
                    if (value.isEmpty()) {
                        continue;
                    }
                    if (ANY_VALUE.equals(value)) {
                        set.clear();
                        set.add(ANY_VALUE);
                        return;
                    }
                    set.add(value);
                }
            }
        }

        private static Pattern initPattern(String patternValue) {
            String ports = null;
            Matcher matcher = PORTS_PATTERN.matcher(patternValue);
            if (matcher.matches()) {
                patternValue = matcher.group(1);
                ports = matcher.group(2);
            }
            patternValue = "\\Q" + patternValue + "\\E";
            patternValue = patternValue.replace("*", "\\E.*\\Q");
            if (ports != null) {
                patternValue += (ANY_VALUE.equals(ports) ? "(:\\d+)?" : ":(" + ports.replace(',', '|') + ")");
            }
            return Pattern.compile(patternValue);
        }

        public Builder allowCredentials(Boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
            return this;
        }

        public Builder allowCredentials(String allowCredentials) {
            if ("true".equals(allowCredentials)) {
                this.allowCredentials = true;
            } else if ("false".equals(allowCredentials)) {
                this.allowCredentials = false;
            }
            return this;
        }

        public Builder maxAge(Long maxAge) {
            if (maxAge != null && maxAge > -1) {
                this.maxAge = maxAge;
            }
            return this;
        }

        public CorsMeta build() {
            int len = allowedOrigins.size();
            String[] origins = new String[len];
            List<Pattern> originsPatterns = new ArrayList<>(len);
            int i = 0;
            for (String origin : allowedOrigins) {
                origins[i++] = origin;
                if (ANY_VALUE.equals(origin)) {
                    continue;
                }
                originsPatterns.add(initPattern(origin));
            }
            return new CorsMeta(
                    origins,
                    originsPatterns.toArray(new Pattern[0]),
                    allowedMethods.toArray(EMPTY_STRING_ARRAY),
                    allowedHeaders.toArray(EMPTY_STRING_ARRAY),
                    exposedHeaders.toArray(EMPTY_STRING_ARRAY),
                    allowCredentials,
                    maxAge);
        }
    }
}
