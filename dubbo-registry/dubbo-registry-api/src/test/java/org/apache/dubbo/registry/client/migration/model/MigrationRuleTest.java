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

public class MigrationRuleTest {

    @Test
    public void test_parse() {
        String rule = "key: demo-consumer\n" +
                "step: APPLICATION_FIRST\n" +
                "threshold: 1.0\n" +
                "proportion: 60\n" +
                "delay: 60\n" +
                "force: false\n" +
                "interfaces:\n" +
                "  - serviceKey: DemoService:1.0.0\n" +
                "    threshold: 0.5\n" +
                "    proportion: 30\n" +
                "    delay: 30\n" +
                "    force: true\n" +
                "    step: APPLICATION_FIRST\n" +
                "  - serviceKey: GreetingService:1.0.0\n" +
                "    step: FORCE_APPLICATION";

        MigrationRule migrationRule = MigrationRule.parse(rule);
        assertEquals("demo-consumer", migrationRule.getKey());
        assertEquals(MigrationStep.APPLICATION_FIRST ,migrationRule.getStep());
        assertEquals(1.0f, migrationRule.getThreshold());
        assertEquals(60, migrationRule.getProportion());
        assertEquals(60, migrationRule.getDelay());
        assertEquals(false, migrationRule.getForce());

        assertEquals(migrationRule.getInterfaces().size(), 2);
        assertNotNull(migrationRule.getInterfaceRule("DemoService:1.0.0"));
        assertNotNull(migrationRule.getInterfaceRule("GreetingService:1.0.0"));

        assertEquals(0.5f, migrationRule.getThreshold("DemoService:1.0.0"));
        assertEquals(30, migrationRule.getProportion("DemoService:1.0.0"));
        assertEquals(30, migrationRule.getDelay("DemoService:1.0.0"));
        assertEquals(true, migrationRule.getForce("DemoService:1.0.0"));
        assertEquals(MigrationStep.APPLICATION_FIRST ,migrationRule.getStep("DemoService:1.0.0"));

        assertEquals(1.0f, migrationRule.getThreshold("GreetingService:1.0.0"));
        assertEquals(60, migrationRule.getProportion("GreetingService:1.0.0"));
        assertEquals(60, migrationRule.getDelay("GreetingService:1.0.0"));
        assertEquals(false, migrationRule.getForce("GreetingService:1.0.0"));
        assertEquals(MigrationStep.FORCE_APPLICATION ,migrationRule.getStep("GreetingService:1.0.0"));
    }
}
