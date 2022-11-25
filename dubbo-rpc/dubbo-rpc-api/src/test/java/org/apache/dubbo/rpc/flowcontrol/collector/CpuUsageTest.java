package org.apache.dubbo.rpc.flowcontrol.collector;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
public class CpuUsageTest {
    private ApplicationModel applicationModel;
    private ServerMetricsCollector serverMetricsCollector;
    private CpuUsage cpuUsage;

    @BeforeEach
    public void setup(){
        ApplicationConfig config = new ApplicationConfig();
        config.setName("MockCpuUsage");
        applicationModel = ApplicationModel.defaultModel();
        applicationModel.getApplicationConfigManager().setApplication(config);
        serverMetricsCollector = new ServerMetricsCollector(ServerMetricsCollector.defaultBucketNum,ServerMetricsCollector.defaultTimeWindowSeconds);
        cpuUsage = new CpuUsage();
        cpuUsage.setApplicationModel(applicationModel);
    }

    @AfterEach
    public void teardown(){
        applicationModel.destroy();
        cpuUsage.destroyPeriodAutoUpdate();
    }



    @Test
    public void testPeriodAutoUpdate() throws InterruptedException {
        cpuUsage.startPeriodAutoUpdate();
        for(int i=0; i < 10; i++){
            Thread.sleep(1000);
            double value = cpuUsage.getCpuUsage();
            Assertions.assertTrue(value != Double.NaN && value >= 0 && value <= 1);
            System.out.println("The epoch is :" + i +" its CPU usage under period auto update is : " + value);
        }
    }


}
