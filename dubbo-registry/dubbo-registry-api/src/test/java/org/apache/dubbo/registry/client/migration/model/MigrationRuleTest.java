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
