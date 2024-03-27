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

import java.util.ArrayList;
import java.util.List;

public class LeafRuleNode implements RuleNode {

    // source
    private String path;

    // principles: val1,val2 ; namespaces: val3,val4
    private List<String> expectedValues;

    public LeafRuleNode(String path, List<String> expectedValues) {
        this.path = path;
        this.expectedValues = expectedValues;
    }

    @Override
    public boolean evaluate(AuthorizationContext context) {
        context.addCurrentPath(path);

        List<String> valuesToValidate = context.getRequestCredential().getByPath(context.getCurrentPath());

        if (valuesToValidate.isEmpty()) {
            return false;
        }

        List<String> l = new ArrayList<>(valuesToValidate);
        l.removeAll(expectedValues);
        return l.isEmpty();
    }

    @Override
    public String getName() {
        return path;
    }
}
