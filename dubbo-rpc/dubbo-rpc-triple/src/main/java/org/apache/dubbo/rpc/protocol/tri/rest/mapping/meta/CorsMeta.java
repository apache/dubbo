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

import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.rpc.protocol.tri.rest.cors.CorsUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;

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

    public CorsMeta combine(CorsMeta other) {
        if (other == null) {
            return this;
        }
        return builder()
                .allowedOrigins(allowedOrigins)
                .allowedOrigins(other.allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowedMethods(other.allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowedHeaders(other.allowedHeaders)
                .exposedHeaders(exposedHeaders)
                .exposedHeaders(other.exposedHeaders)
                .allowCredentials(other.allowCredentials == null ? allowCredentials : other.allowCredentials)
                .maxAge(other.maxAge == null ? maxAge : other.maxAge)
                .build();
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
                if (value == null) {
                    continue;
                }
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

        public Builder applyDefault() {
            if (allowedOrigins.isEmpty()) {
                allowedOrigins.add(ANY_VALUE);
            }
            if (allowedHeaders.isEmpty()) {
                allowedHeaders.add(ANY_VALUE);
            }
            if (maxAge == null) {
                maxAge = 1800L;
            }
            return this;
        }

        public CorsMeta build() {
            if (allowedOrigins.isEmpty()
                    && allowedMethods.isEmpty()
                    && allowedHeaders.isEmpty()
                    && exposedHeaders.isEmpty()
                    && allowCredentials == null
                    && maxAge == null) {
                return null;
            }

            int len = allowedOrigins.size();
            String[] origins = new String[len];
            Pattern[] originsPatterns = new Pattern[len];
            int i = 0;
            for (String origin : allowedOrigins) {
                origins[i] = origin;
                originsPatterns[i] = ANY_VALUE.equals(origin) ? null : initPattern(origin);
                i++;
            }
            if (allowedMethods.isEmpty()) {
                allowedMethods.add(HttpMethods.GET.name());
                allowedMethods.add(HttpMethods.HEAD.name());
                allowedMethods.add(HttpMethods.POST.name());
            }
            return new CorsMeta(
                    origins,
                    originsPatterns,
                    allowedMethods.toArray(EMPTY_STRING_ARRAY),
                    allowedHeaders.toArray(EMPTY_STRING_ARRAY),
                    exposedHeaders.toArray(EMPTY_STRING_ARRAY),
                    allowCredentials,
                    maxAge);
        }
    }
}
