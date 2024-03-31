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
import java.util.List;

public class LeafRuleNode implements RuleNode {

    /**
     * e.g principle in rules.from.source.principles
     */
    private String rulePropName;

    /**
     * patterns that matches required values
     */
    private List<String> expectedValuePattern;

    public LeafRuleNode(String nodeName, List<String> expectedValue) {
        this.rulePropName = nodeName;
        this.expectedValuePattern = parseToPattern(expectedValue);
    }

    @Override
    public boolean evaluate(AuthorizationRequestContext context) {
        context.addCurrentPath(rulePropName);

        List<String> valuesToValidate = context.getRequestCredential().getByPath(context.getCurrentPath());
        if (valuesToValidate.isEmpty()) {
            context.removeCurrentPath();
            return false;
        }

        List<String> l = new ArrayList<>(valuesToValidate);
        for (String p:expectedValuePattern) {
            //If we have multiple values to validate, then every value must match at list one rule pattern
            l.removeIf(val -> val.matches(p));
            if(l.isEmpty()){
                break;
            }
        }
        context.removeCurrentPath();
        return l.isEmpty();
    }

    @Override
    public String getNodeName() {
        return rulePropName;
    }

    private List<String> parseToPattern(List<String> values){
        List<String> pattern = new ArrayList<>(1);
        for (String val: values) {
            StringBuilder patternBuilder = new StringBuilder();
            for (int i = 0; i < val.length(); i++) {
                char c = val.charAt(i);
                switch (c) {
                    case '*':
                        patternBuilder.append(".*");
                        break;
                    case '\\':
                    case '.':
                    case '^':
                    case '$':
                    case '+':
                    case '?':
                    case '{':
                    case '}':
                    case '[':
                    case ']':
                    case '|':
                    case '(':
                    case ')':
                        patternBuilder.append("\\").append(c);
                        break;
                    default:
                        patternBuilder.append(c);
                        break;
                }
            }
            pattern.add(patternBuilder.toString());
        }
        return pattern;
    }

}
