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
package org.apache.dubbo.xds.security.authz.rule;

import org.apache.dubbo.xds.security.authz.RuleSource;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot.Action;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleTreeBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 默认规则工厂
 */
public class DefaultRuleFactory implements RuleFactory {

    @Override
    public List<RuleRoot> getRules(RuleSource ruleSource) {

        Map<String, Object> ruleMap = ruleSource.readAsMap();

        Action action = Action.map((String) ruleMap.get("action"));
        if(action == null){
             throw new RuntimeException("Parse rule map failed: unknown action");
        }

        RuleTreeBuilder builder = new RuleTreeBuilder("rules");
        builder.createFromMap(ruleMap);

        return Arrays.asList(builder.getRoot(Relation.AND, action));
    }
}
