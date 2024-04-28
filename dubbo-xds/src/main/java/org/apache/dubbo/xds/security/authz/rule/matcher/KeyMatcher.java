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
import org.apache.dubbo.xds.security.authz.rule.matcher.StringMatcher.MatchType;

import java.util.Map;

public class KeyMatcher implements Matcher<Map<String, String>> {

    private String key;

    private StringMatcher stringMatcher;

    public KeyMatcher(MatchType matchType, String condition, RequestAuthProperty authProperty, String key) {
        this.stringMatcher = new StringMatcher(matchType, condition, authProperty);
        this.key = key;
    }

    public KeyMatcher(String key, StringMatcher stringMatcher) {
        this.key = key;
        this.stringMatcher = stringMatcher;
    }

    @Override
    public boolean match(Map<String, String> actual) {
        if (actual == null) {
            return this.stringMatcher.match(null);
        }
        String toMatch = actual.get(key);
        if (toMatch == null) {
            return false;
        }
        return this.stringMatcher.match(toMatch);
    }

    @Override
    public RequestAuthProperty propType() {
        return this.stringMatcher.propType();
    }

    @Override
    public String toString() {
        return "KeyMatcher{" + "key='" + key + '\'' + ", stringMatcher=" + stringMatcher + '}';
    }
}
