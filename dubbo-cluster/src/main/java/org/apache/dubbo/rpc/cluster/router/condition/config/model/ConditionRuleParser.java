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

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

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

    public static ConditionRouterRule parse(String rawRule) {
        Constructor constructor = new Constructor(ConditionRouterRule.class);

        Yaml yaml = new Yaml(constructor);
        ConditionRouterRule rule = yaml.load(rawRule);
        rule.setRawRule(rawRule);
        if (CollectionUtils.isEmpty(rule.getConditions())) {
            rule.setValid(false);
        }

        BlackWhiteListRule blackWhiteList = rule.getBlackWhiteList();
        if (blackWhiteList != null && CollectionUtils.isEmpty(blackWhiteList.getConditions())) {
            blackWhiteList.setValid(false);
        }
        return rule;
    }

}
