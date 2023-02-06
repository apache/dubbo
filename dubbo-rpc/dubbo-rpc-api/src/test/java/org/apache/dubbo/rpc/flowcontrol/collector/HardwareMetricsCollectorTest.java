package org.apache.dubbo.rpc.flowcontrol.collector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HardwareMetricsCollectorTest {
    @Test
    public void testSystemUsage() throws Exception{
        HardwareMetricsCollector hardwareMetricsCollector = new HardwareMetricsCollector();
        for(int i = 1; i <= 4; i++){
            Thread.sleep(500);
            Assertions.assertTrue(hardwareMetricsCollector.systemCpuUsage() >= 0.0);
        }
    }
}
