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
package org.apache.dubbo.rpc.cluster.router.expression.model;

import java.util.List;

/**
 * A ruleset for some provider.
 * @author Weihua
 * @since 2.7.8
 */
public class RuleSet {

    /**
     * Whether the ruleSet is enabled or not, set true as its default value.
     */
    private boolean enabled = true;

    /**
     * Whether default rule is enabled, set false as its default value.
     * This is useful when none of the provider is found after evaluating all the rules.
     * If this is set to false, exception of no provider will be thrown.
     * If this is set to true, all the left providers will be chosen, just like the rule of following:
     * clientCondition: true
     * serverQuery: true
     */
    private boolean defaultRuleEnabled;

    /**
     * The rules are in order. The top one will be evaluated in top priority.
     */
    private List<Rule> rules;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDefaultRuleEnabled() {
        return defaultRuleEnabled;
    }

    public void setDefaultRuleEnabled(boolean defaultRuleEnabled) {
        this.defaultRuleEnabled = defaultRuleEnabled;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public String toString(){
        return "RuleSet(enabled=" + enabled
                + ", defaultRuleEnabled=" + defaultRuleEnabled
                + ",rules=" + rules + ")";
    }
}
