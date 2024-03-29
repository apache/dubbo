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

import org.apache.dubbo.xds.security.authz.AuthorizationContext;

public class RuleRoot implements RuleNode {

    /**
     * root之间的关系。所有Relation=AND的树进行AND，所有Relation=OR的树进行OR
     */
    private Relation relationToRoots;

    private CompositeRuleNode root;

    private Action action;

    public RuleRoot(Relation relationToOtherRoots, CompositeRuleNode root, Action action) {
        this.relationToRoots = relationToOtherRoots;
        this.root = root;
        this.action = action;
    }

    public Relation getRelationToRoots() {
        return relationToRoots;
    }

    public CompositeRuleNode getRoot() {
        return root;
    }

    @Override
    public boolean evaluate(AuthorizationContext context) {
        return root.evaluate(context);
    }

    @Override
    public String getName() {
        return "root";
    }

    public Action getAction(){
        return action;
    }

    /**
     * The action of authorization policy
     */
    public enum Action{
        /**
         * The request must map this policy
         */
        ALLOW("ALLOW",true),

        /**
         * The request must not map this policy
         */
        DENY("DENY",false);

        private final String name;

        private boolean boolVal;

        Action(String name,boolean boolValue){
            this.name = name;
            this.boolVal = boolValue;
        }

        public static Action map(String name){
            name = name.toUpperCase();
            switch (name){
                case "ALLOW" :
                    return ALLOW;
                case "DENY":
                    return DENY;
            }
            return null;
        }

        public boolean boolVal(){
            return boolVal;
        }

    }


}
