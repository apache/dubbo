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
package org.apache.dubbo.registry.client.migration.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MigrationRuleTest {

    @Test
    public void test_parse() {
        String rule = "key: demo-consumer\n" +
                "step: APPLICATION_FIRST\n" +
                "threshold: 1.0\n" +
                "interfaces:\n" +
                "  - serviceKey: DemoService:1.0.0\n" +
                "    threshold: 1.0\n" +
                "    step: APPLICATION_FIRST\n" +
                "  - serviceKey: GreetingService:1.0.0\n" +
                "    step: FORCE_APPLICATION";

        MigrationRule migrationRule = MigrationRule.parse(rule);
        assertEquals(migrationRule.getKey(), "demo-consumer");
        assertEquals(migrationRule.getStep(), MigrationStep.APPLICATION_FIRST);
        assertEquals(migrationRule.getThreshold(), 1.0f);
        assertEquals(migrationRule.getInterfaces().size(), 2);
        assertNotNull(migrationRule.getInterfaceRule("DemoService:1.0.0"));
        assertNotNull(migrationRule.getInterfaceRule("GreetingService:1.0.0"));
        assertNull(migrationRule.getApplications());
    }
}
