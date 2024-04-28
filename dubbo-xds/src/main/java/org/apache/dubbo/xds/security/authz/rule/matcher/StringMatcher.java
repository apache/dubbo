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
package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import java.util.Objects;
import java.util.regex.Pattern;

public class StringMatcher implements Matcher<String> {

    private String condition;

    private MatchType matchType;

    private RequestAuthProperty authProperty;

    private ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(StringMatcher.class);

    private boolean not = false;

    public StringMatcher(MatchType matchType, String condition, RequestAuthProperty authProperty) {
        this.matchType = matchType;
        this.condition = condition;
        this.authProperty = authProperty;
    }

    public StringMatcher(MatchType matchType, String condition, RequestAuthProperty authProperty, boolean not) {
        this.matchType = matchType;
        this.condition = condition;
        this.authProperty = authProperty;
        this.not = not;
    }

    public boolean match(String actual) {
        boolean res;
        if (StringUtils.isEmpty(actual)) {
            return Objects.equals(condition, actual);
        } else {
            switch (matchType) {
                case EXACT:
                    res = actual.equals(condition);
                    break;
                case PREFIX:
                    res = actual.startsWith(condition);
                    break;
                case SUFFIX:
                    res = actual.endsWith(condition);
                    break;
                case CONTAIN:
                    res = actual.contains(condition);
                    break;
                case REGEX:
                    try {
                        res = Pattern.matches(condition, actual);
                        break;
                    } catch (Exception e) {
                        logger.warn("", "", "", "Irregular matching,key={},str={}", e);
                        return false;
                    }
                default:
                    throw new UnsupportedOperationException("unsupported string compare operation");
            }
        }
        return not ^ res;
    }

    @Override
    public RequestAuthProperty propType() {
        return authProperty;
    }

    @Override
    public String toString() {
        return "StringMatcher{" + "condition='" + condition + '\'' + ", matchType=" + matchType + ", authProperty="
                + authProperty + ", logger=" + logger + ", not=" + not + '}';
    }

    public enum MatchType {

        /**
         * exact match.
         */
        EXACT("exact"),
        /**
         * prefix match.
         */
        PREFIX("prefix"),
        /**
         * suffix match.
         */
        SUFFIX("suffix"),
        /**
         * regex match.
         */
        REGEX("regex"),
        /**
         * contain match.
         */
        CONTAIN("contain");

        /**
         * type of matcher.
         */
        public final String key;

        MatchType(String type) {
            this.key = type;
        }

        @Override
        public String toString() {
            return this.key;
        }
    }
}
