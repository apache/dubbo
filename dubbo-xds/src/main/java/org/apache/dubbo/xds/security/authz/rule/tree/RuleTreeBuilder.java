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
package org.apache.dubbo.xds.security.authz.rule.tree;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation;
import org.apache.dubbo.xds.security.authz.rule.tree.RuleRoot.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * non-thread safe
 */
public class RuleTreeBuilder {

    /**
     * Root of the rule tree.
     */
    private RuleRoot root;

    /**
     * The relations between nodes that have same parent. <p>
     * eg: <p>
     * 1. <code>rules[0].from</code> AND <code>rules[0].to</code>
     * Only when the request meet all FROM AND TO rule,their parent (rules[0]) will returns true.
     * <p>
     * 2. <code>rules[0].from[0].source[0].principles[0]</code> OR <code>rules[0].from[0].source[0].namespaces[0]</code>
     * Only when the request meet PRINCIPLE OR NAMESPACE rule, their parent (source[0]) will returns true.
     * <p>
     * The node in same level shares same relation, like <code>rules.from</code> and <code>rules.to</code> because they are in same level (2).
     */
    private List<Relation> nodeLevelRelations = new ArrayList<>();

    public RuleTreeBuilder(Relation relationToOtherRoots, Action action) {
        this.root = new RuleRoot(relationToOtherRoots, action);
    }

    public void createFromMap(Map<String, Object> map) {
        if (CollectionUtils.isEmpty(nodeLevelRelations)) {
            throw new RuntimeException("Node level relations can't be null or empty");
        }

        for (String key : map.keySet()) {
            Object value = map.get(key);
            processNode(root, key, value, 0);
        }
    }

    public void setPathLevelRelations(List<Relation> pathLevelRelations) {
        this.nodeLevelRelations = pathLevelRelations;
    }

    private void processNode(CompositeRuleNode parent, String key, Object value, int level) {
        // key：name of current node
        // value：values for children of current node
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {

                if (list.get(0) instanceof String) {
                    LeafRuleNode current = new LeafRuleNode(key, (List<String>) list);
                    parent.addChild(key, current);
                } else if (list.get(0) instanceof Map) {
                    CompositeRuleNode current = new CompositeRuleNode(key, nodeLevelRelations.get(level));
                    parent.addChild(key, current);

                    for (Object item : list) {
                        ((Map<?, ?>) item)
                                .forEach((childKey, childValue) ->
                                        processNode(current, (String) childKey, childValue, level + 1));
                    }
                }
            }
        } else if (value instanceof Map) {
            CompositeRuleNode current = new CompositeRuleNode(key, nodeLevelRelations.get(level));
            parent.addChild(key, current);
            ((Map<?, ?>) value)
                    .forEach((childKey, childValue) -> processNode(current, (String) childKey, childValue, level + 1));
        } else {
            throw new RuntimeException();
        }
    }

    public RuleRoot getRoot() {
        return root;
    }
}