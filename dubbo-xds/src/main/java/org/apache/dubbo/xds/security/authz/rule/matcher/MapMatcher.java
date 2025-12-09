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

import java.util.Map;

/**
 * supports multiple keys and values
 */
public class MapMatcher implements Matcher<Map<String, String>> {

    private Map<String, Matcher<String>> keyToMatchers;

    private RequestAuthProperty property;

    public MapMatcher(Map<String, Matcher<String>> matcherMap, RequestAuthProperty property) {
        this.keyToMatchers = matcherMap;
        this.property = property;
    }

    @Override
    public boolean match(Map<String, String> actualValues) {
        for (String key : keyToMatchers.keySet()) {
            Matcher<String> matcher = keyToMatchers.get(key);
            String actual = actualValues.get(key);
            if (!matcher.match(actual)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public RequestAuthProperty propType() {
        return property;
    }

    @Override
    public String toString() {
        return "MapMatcher{" + "keyToMatchers=" + keyToMatchers + ", property=" + property + '}';
    }
}
