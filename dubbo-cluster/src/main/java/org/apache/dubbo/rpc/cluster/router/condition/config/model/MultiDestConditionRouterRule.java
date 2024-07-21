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

import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.rpc.cluster.router.AbstractRouterRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.rpc.cluster.Constants.CONDITIONS_KEY;

public class MultiDestConditionRouterRule extends AbstractRouterRule {

    private List<MultiDestCondition> conditions;

    public static AbstractRouterRule parseFromMap(Map<String, Object> map) {

        MultiDestConditionRouterRule multiDestConditionRouterRule = new MultiDestConditionRouterRule();
        multiDestConditionRouterRule.parseFromMap0(map);
        List<Map<String, String>> conditions = (List<Map<String, String>>) map.get(CONDITIONS_KEY);
        List<MultiDestCondition> multiDestConditions = new ArrayList<>();

        for (Map<String, String> condition : conditions) {
            multiDestConditions.add((MultiDestCondition) JsonUtils.convertObject(condition, MultiDestCondition.class));
        }
        multiDestConditionRouterRule.setConditions(multiDestConditions);

        return multiDestConditionRouterRule;
    }

    public List<MultiDestCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<MultiDestCondition> conditions) {
        this.conditions = conditions;
    }
}
