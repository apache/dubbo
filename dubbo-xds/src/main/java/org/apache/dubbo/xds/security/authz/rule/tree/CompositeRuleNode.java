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

import org.apache.dubbo.xds.security.authz.AuthorizationRequestContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositeRuleNode implements RuleNode {

    protected String name;

    protected Map<String, List<RuleNode>> children;

    protected Relation relation;

    public CompositeRuleNode(String name, Map<String, List<RuleNode>> children, Relation relation) {
        this.name = name;
        this.children = children;
        this.relation = relation;
    }

    public CompositeRuleNode(String name, Relation relation) {
        this.name = name;
        this.relation = relation;
        this.children = new HashMap<>();
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }

    public void addChild(RuleNode ruleNode) {
        this.children
                .computeIfAbsent(ruleNode.getNodeName(), (k) -> new ArrayList<>())
                .add(ruleNode);
    }

    public Relation getRelation() {
        return relation;
    }

    @Override
    public boolean evaluate(AuthorizationRequestContext context) {
        boolean result;
        context.depthIncrease();
        if (context.enableTrace()) {
            context.addTraceInfo("<rules name:" + name + ">");
        }

        if (relation == Relation.AND) {
            result = children.values().stream()
                    .allMatch(childList -> childList.stream().allMatch(ch -> ch.evaluate(context)));
        } else if (relation == Relation.OR) {
            result = children.values().stream()
                    .anyMatch(childList -> childList.stream().anyMatch(ch -> ch.evaluate(context)));
        } else {
            // relation == NOT
            result = children.values().stream()
                    .noneMatch(childList -> childList.stream().anyMatch(ch -> ch.evaluate(context)));
        }
        if (context.enableTrace()) {
            String msg = "<rules name:" + name + "> " + (result ? "match " : "not match ");
            context.addTraceInfo(msg);
        }
        context.depthDecrease();
        return result;
    }

    public Map<String, List<RuleNode>> getChildren() {
        return children;
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return "CompositeRuleNode{" + "name='" + name + '\'' + ", children=" + children + ", relation=" + relation
                + '}';
    }
}
