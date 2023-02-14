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
package org.apache.dubbo.rpc.cluster.router.condition.matcher;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.router.condition.matcher.pattern.ValuePattern;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CLUSTER_FAILED_EXEC_CONDITION_ROUTER;

/**
 * The abstract implementation of ConditionMatcher, records the match and mismatch patterns of this matcher while at the same time
 * provides the common match logics.
 */
public abstract class AbstractConditionMatcher implements ConditionMatcher {
    public static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractConditionMatcher.class);
    public static final String DOES_NOT_FOUND_VALUE = "dubbo_internal_not_found_argument_condition_value";
    final Set<String> matches = new HashSet<>();
    final Set<String> mismatches = new HashSet<>();
    private final ModuleModel model;
    private final List<ValuePattern> valueMatchers;
    protected final String key;

    public AbstractConditionMatcher(String key, ModuleModel model) {
        this.key = key;
        this.model = model;
        this.valueMatchers = model.getExtensionLoader(ValuePattern.class).getActivateExtensions();
    }

    public static String getSampleValueFromUrl(String conditionKey, Map<String, String> sample, URL param, Invocation invocation) {
        String sampleValue;
        //get real invoked method name from invocation
        if (invocation != null && (METHOD_KEY.equals(conditionKey) || METHODS_KEY.equals(conditionKey))) {
            sampleValue = invocation.getMethodName();
        } else {
            sampleValue = sample.get(conditionKey);
        }

        return sampleValue;
    }

    public boolean isMatch(Map<String, String> sample, URL param, Invocation invocation, boolean isWhenCondition) {
        String value = getValue(sample, param, invocation);
        if (value == null) {
            // if key does not present in whichever of url, invocation or attachment based on the matcher type, then return false.
            return false;
        }

        if (!matches.isEmpty() && mismatches.isEmpty()) {
            for (String match : matches) {
                if (doPatternMatch(match, value, param, invocation, isWhenCondition)) {
                    return true;
                }
            }
            return false;
        }

        if (!mismatches.isEmpty() && matches.isEmpty()) {
            for (String mismatch : mismatches) {
                if (doPatternMatch(mismatch, value, param, invocation, isWhenCondition)) {
                    return false;
                }
            }
            return true;
        }

        if (!matches.isEmpty() && !mismatches.isEmpty()) {
            //when both mismatches and matches contain the same value, then using mismatches first
            for (String mismatch : mismatches) {
                if (doPatternMatch(mismatch, value, param, invocation, isWhenCondition)) {
                    return false;
                }
            }
            for (String match : matches) {
                if (doPatternMatch(match, value, param, invocation, isWhenCondition)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public Set<String> getMatches() {
        return matches;
    }

    @Override
    public Set<String> getMismatches() {
        return mismatches;
    }

    // range, equal or other methods
    protected boolean doPatternMatch(String pattern, String value, URL url, Invocation invocation, boolean isWhenCondition) {
        for (ValuePattern valueMatcher : valueMatchers) {
            if (valueMatcher.shouldMatch(pattern)) {
                return valueMatcher.match(pattern, value, url, invocation, isWhenCondition);
            }
        }
        // this should never happen.
        logger.error(CLUSTER_FAILED_EXEC_CONDITION_ROUTER, "Executing condition rule value match expression error.", "pattern is " + pattern + ", value is " + value + ", condition type " + (isWhenCondition ? "when" : "then"), "There should at least has one ValueMatcher instance that applies to all patterns, will force to use wildcard matcher now.");

        ValuePattern paramValueMatcher = model.getExtensionLoader(ValuePattern.class).getExtension("wildcard");
        return paramValueMatcher.match(pattern, value, url, invocation, isWhenCondition);
    }

    /**
     * Used to get value from different places of the request context, for example, url, attachment and invocation.
     * This makes condition rule possible to check values in any place of a request.
     */
    protected abstract String getValue(Map<String, String> sample, URL url, Invocation invocation);

}
