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

import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

/**
 * Supports simple '*' match
 */
public class WildcardStringMatcher implements Matcher<String> {

    private String value;

    private RequestAuthProperty authProperty;

    public WildcardStringMatcher(String value, RequestAuthProperty authProperty) {
        this.value = parseToPattern(value);
        this.authProperty = authProperty;
    }

    @Override
    public boolean match(String actual) {
        String pattern = parseToPattern(value);
        return actual.matches(pattern);
    }

    private String parseToPattern(String val) {
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            switch (c) {
                case '*':
                    patternBuilder.append(".*");
                    break;
                case '\\':
                case '.':
                case '^':
                case '$':
                case '+':
                case '?':
                case '{':
                case '}':
                case '[':
                case ']':
                case '|':
                case '(':
                case ')':
                    patternBuilder.append("\\").append(c);
                    break;
                default:
                    patternBuilder.append(c);
                    break;
            }
        }
        return patternBuilder.toString();
    }

    @Override
    public RequestAuthProperty propType() {
        return authProperty;
    }
}
