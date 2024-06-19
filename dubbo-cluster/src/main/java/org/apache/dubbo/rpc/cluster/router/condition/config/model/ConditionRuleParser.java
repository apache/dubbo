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
package org.apache.dubbo.rpc.cluster.router.condition.config.model;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;

import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import static org.apache.dubbo.rpc.cluster.Constants.CONFIG_VERSION_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_VERSION_V31;

/**
 * %YAML1.2
 *
 * scope: application
 * runtime: true
 * force: false
 * conditions:
 *   - >
 *     method!=sayHello =>
 *   - >
 *     ip=127.0.0.1
 *     =>
 *     1.1.1.1
 */
public class ConditionRuleParser {

    public static AbstractRouterRule parse(String rawRule) {
        AbstractRouterRule rule;
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> map = yaml.load(rawRule);
        String confVersion = (String) map.get(CONFIG_VERSION_KEY);

        if (confVersion != null && confVersion.startsWith(RULE_VERSION_V31)) {
            rule = MultiDestConditionRouterRule.parseFromMap(map);
            if (CollectionUtils.isEmpty(((MultiDestConditionRouterRule) rule).getConditions())) {
                rule.setValid(false);
            }
        } else {
            rule = ConditionRouterRule.parseFromMap(map);
            if (CollectionUtils.isEmpty(((ConditionRouterRule) rule).getConditions())) {
                rule.setValid(false);
            }
        }
        rule.setRawRule(rawRule);

        return rule;
    }
}
