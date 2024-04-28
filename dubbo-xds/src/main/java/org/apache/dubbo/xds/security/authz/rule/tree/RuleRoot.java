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

public class RuleRoot extends CompositeRuleNode {

    /**
     * Relations between rule tree roots.
     * All roots that has Relation=AND will do AND, and all roots has Relation=OR will do OR.
     */
    private Action action;

    public RuleRoot(Relation relation, Action action, String name) {
        super(name, relation);
        this.action = action;
    }

    public RuleRoot(Relation relation, Action action) {
        super("", relation);
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public boolean evaluate(AuthorizationRequestContext context) {
        boolean result;
        if (context.enableTrace()) {
            String msg = "<root> ";
            context.addTraceInfo(msg);
        }
        if (relation == Relation.AND) {
            result = children.values().stream()
                    .allMatch(childList -> childList.stream().allMatch(ch -> ch.evaluate(context)));
        } else {
            // Relation == OR
            result = children.values().stream()
                    .anyMatch(childList -> childList.stream().anyMatch(ch -> ch.evaluate(context)));
        }
        if (context.enableTrace()) {
            String msg = "<root> " + (result ? "match" : "not match, action:" + action);
            context.addTraceInfo(msg);
        }
        return result;
    }

    /**
     * The action of authorization policy
     */
    public enum Action {

        /**
         * The request must map this policy
         */
        ALLOW("ALLOW", true),

        /**
         * The request must not map this policy
         */
        DENY("DENY", false),

        /**
         * Only log this policy, will not affect the result
         */
        LOG("LOG", false);

        private final String name;

        private boolean boolVal;

        Action(String name, boolean boolValue) {
            this.name = name;
            this.boolVal = boolValue;
        }

        public static Action map(String name) {
            name = name.toUpperCase();
            switch (name) {
                case "ALLOW":
                    return ALLOW;
                case "DENY":
                    return DENY;
                case "LOG":
                    return LOG;
                default:
                    return null;
            }
        }

        public boolean boolVal() {
            return boolVal;
        }
    }

    @Override
    public String toString() {
        return "RuleRoot{" + "action=" + action + "} " + super.toString();
    }
}
