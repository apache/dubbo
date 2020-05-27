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

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.stream.Collectors;

/**
 * A yaml constructor for parsing RuleSets which should be a map.
 *
 * @author Weihua
 * @since 1.0.0
 */
public class ExpressionRuleConstructor extends Constructor {

    private TypeDescription itemType = new TypeDescription(RuleSet.class);

    private static final String ROOT_NAME = "ruleSetRoot";

    public ExpressionRuleConstructor() {
        this.rootTag = new Tag(ROOT_NAME);
        this.addTypeDescription(itemType);
    }

    @Override
    protected Object constructObject(Node node) {
        if (ROOT_NAME.equals(node.getTag().getValue()) && node instanceof MappingNode) {
            MappingNode mNode = (MappingNode) node;
            return mNode.getValue().stream().collect(
                Collectors.toMap(
                    t -> super.constructObject(t.getKeyNode()),
                    t -> {
                        Node child = t.getValueNode();
                        child.setType(itemType.getType());
                        return super.constructObject(child);
                    }
                )
            );
        } else {
            return super.constructObject(node);
        }
    }
}
