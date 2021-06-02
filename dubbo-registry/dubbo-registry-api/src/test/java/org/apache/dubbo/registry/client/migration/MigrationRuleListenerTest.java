package org.apache.dubbo.registry.client.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MigrationRuleListenerTest {
    @Test
    public void test() throws InterruptedException {
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

        DynamicConfiguration dynamicConfiguration = Mockito.mock(DynamicConfiguration.class);

        ApplicationModel.getEnvironment().setDynamicConfiguration(dynamicConfiguration);
        ApplicationModel.getEnvironment().setLocalMigrationRule(rule);
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-consumer");
        ApplicationModel.getConfigManager().setApplication(applicationConfig);

        URL consumerURL = Mockito.mock(URL.class);
        Mockito.when(consumerURL.getServiceKey()).thenReturn("Test");
        Mockito.when(consumerURL.getParameter("timestamp")).thenReturn("1");

        System.setProperty("dubbo.application.migration.delay", "100");
        MigrationRuleHandler handler = Mockito.mock(MigrationRuleHandler.class);

        MigrationRuleListener migrationRuleListener = new MigrationRuleListener();
        migrationRuleListener.getHandlers().put("Test1", handler);

        Mockito.verify(handler, Mockito.timeout(5000)).doMigrate(Mockito.any());

        migrationRuleListener.onRefer(null, null, consumerURL, null);
        Mockito.verify(handler, Mockito.times(2)).doMigrate(Mockito.any());

        ApplicationModel.reset();
    }
}
