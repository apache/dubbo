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

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.xds.security.authz.AuthorizationRequestContext;
import org.apache.dubbo.xds.security.authz.rule.matcher.Matcher;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unchecked,rawtypes")
public class LeafRuleNode implements RuleNode {

    /**
     * e.g principle in rules.from.source.principles
     */
    private String rulePropName;

    /**
     * patterns that matches required values
     */
    private List<Matcher> matchers;

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(LeafRuleNode.class);

    public LeafRuleNode(List<? extends Matcher> expectedConditions, String name) {
        this.matchers = (List<Matcher>) expectedConditions;
        this.rulePropName = name;
    }

    public LeafRuleNode(Matcher matcher, String name) {
        this.matchers = Collections.singletonList(matcher);
        this.rulePropName = name;
    }

    @Override
    public boolean evaluate(AuthorizationRequestContext context) {
        // If we have multiple values to validate, then every value must match at list one rule pattern
        for (Matcher matcher : matchers) {

            Object toValidate = context.getRequestCredential().getRequestProperty(matcher.propType());

            if (!matcher.match(toValidate)) {
                LOGGER.info("principal="+toValidate+" does not match rule "+matcher);
                return false;
            }
            LOGGER.info("principal="+toValidate+" successful match rule"+matcher);
        }

        return true;
    }

    @Override
    public String getNodeName() {
        return rulePropName;
    }
}
