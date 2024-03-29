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

import org.apache.dubbo.xds.security.authz.rule.tree.RuleNode.Relation;

import java.util.List;
import java.util.Map;

public class RuleTreeBuilder {
    private CompositeRuleNode root;

    public RuleTreeBuilder(String rootName) {
        // 顶层默认OR关系
        this.root = new CompositeRuleNode(rootName, RuleNode.Relation.OR);
    }

    public void createFromMap(Map<String, Object> map) {
        for (String key : map.keySet()) {
            Object value = map.get(key);
            processNode(root, key, value);
        }
    }

    private void processNode(CompositeRuleNode parent, String key, Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (!list.isEmpty()) {

                // leaf结点
                if (list.get(0) instanceof String) {
                    parent.addChild(key, new LeafRuleNode(key, (List<String>) list));
                }
                // 非leaf结点
                else if (list.get(0) instanceof Map) {
                    for (Object item : list) {
                        CompositeRuleNode current = new CompositeRuleNode(key, RuleNode.Relation.AND);
                        parent.addChild(key, current);
                        ((Map<?, ?>) item)
                                .forEach((childKey, childValue) -> processNode(current, (String) childKey, childValue));
                    }
                }
            }
        } else if (value instanceof Map) {
            // leaf的前一层
            CompositeRuleNode current = new CompositeRuleNode(key, RuleNode.Relation.OR);
            parent.addChild(key, current);
            ((Map<?, ?>) value).forEach((childKey, childValue) -> processNode(current, (String) childKey, childValue));
        } else {
            throw new RuntimeException();
        }
    }

    // 根据path修改所有同path记录的Relation
    public void changeRelationByPath(String path, RuleNode.Relation newRelation) {
        CompositeRuleNode node = getNodeByPath(path);
        if (node != null) {
            // 更新找到的节点的关系
            node.setRelation(newRelation);
        }
    }

    // 获取路径指定的节点
    private CompositeRuleNode getNodeByPath(String path) {
        String[] segments = path.split("\\.");
        CompositeRuleNode current = root;
        for (String segment : segments) {
            if (current == null) {
                return null;
            }
            //            current = current.getName(segment);
        }
        return current;
    }

    public RuleRoot getRoot(Relation relationToOtherRoots, RuleRoot.Action action) {
        return new RuleRoot(relationToOtherRoots, root, action);
    }

    // CompositeRuleNode 中添加 setRelation 方法
}
