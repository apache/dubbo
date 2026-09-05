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
package org.apache.dubbo.rpc.cluster.router.condition.config;

import org.apache.dubbo.rpc.cluster.router.condition.config.model.ConditionRuleParser;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConditionRuleParserTest {

    @Test
    public void testRejectNonMappingRule() {
        for (String rawRule : Arrays.asList("", " ", "# comment", "---", "null", "[]", "condition")) {
            IllegalArgumentException exception =
                    Assertions.assertThrows(IllegalArgumentException.class, () -> ConditionRuleParser.parse(rawRule));
            Assertions.assertEquals("Condition router rule must be a YAML mapping.", exception.getMessage());
        }
    }
}
