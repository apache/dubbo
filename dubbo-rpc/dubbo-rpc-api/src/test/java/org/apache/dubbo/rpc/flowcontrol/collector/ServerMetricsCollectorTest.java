package org.apache.dubbo.rpc.flowcontrol.collector;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.flowcontrol.DemoService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerMetricsCollectorTest {
    @Test
    public void testUpdateAndGet(){
        ServerMetricsCollector serverMetricsCollector = new ServerMetricsCollector(12,1);
        serverMetricsCollector.updateCount(1L);
        Assertions.assertEquals(serverMetricsCollector.getCount(),1L);
        serverMetricsCollector.updateMinLatency(1L);
        serverMetricsCollector.updateMinLatency(2L);
        Assertions.assertEquals(serverMetricsCollector.getMinLatency(),1L);
        serverMetricsCollector.updateMaxLatency(1L);
        serverMetricsCollector.updateMaxLatency(2L);
        Assertions.assertEquals(serverMetricsCollector.getMaxLatency(),2L);
        serverMetricsCollector.updateSucceedElapsed(10L);
        Assertions.assertEquals(serverMetricsCollector.getSucceedElapsed(),10L);
        serverMetricsCollector.updateMaxQPS(1.0);
        serverMetricsCollector.updateMaxQPS(2.0);
        Assertions.assertEquals(serverMetricsCollector.getMaxQPS(),2.0);
    }

    @Test
    public void testEndCount(){
        ServerMetricsCollector serverMetricsCollector = new ServerMetricsCollector(ServerMetricsCollector.defaultBucketNum, ServerMetricsCollector.defaultTimeWindowSeconds);
        serverMetricsCollector.end(1000L);
        serverMetricsCollector.end(2000L);
        serverMetricsCollector.end(3000L);

        Assertions.assertEquals(serverMetricsCollector.getSucceedElapsed(),6000);
        Assertions.assertEquals(serverMetricsCollector.getCount(),3);
        Assertions.assertEquals(serverMetricsCollector.getMinLatency(),1000);
        Assertions.assertEquals(serverMetricsCollector.getMaxLatency(),3000);
        Assertions.assertTrue(serverMetricsCollector.getMaxQPS() > 0.0);
    }

    @Test
    public void testEndCountInMultiThread() throws Exception{
        ServerMetricsCollector serverMetricsCollector = new ServerMetricsCollector(ServerMetricsCollector.defaultBucketNum,ServerMetricsCollector.defaultTimeWindowSeconds);
        int threadNum = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadNum);
        List<Thread> threadList = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++){
            Thread thread = new Thread(() -> {
               try{
                   startLatch.await();
                   for(int j = 0; j < 100; j++){
                       serverMetricsCollector.end(1000L);
                       successCount.incrementAndGet();
                   }
                   endLatch.countDown();
               }catch (Exception e){
                   e.printStackTrace();
               }
            });
            threadList.add(thread);
        }

        threadList.forEach(Thread::start);
        startLatch.countDown();
        endLatch.await();
        Assertions.assertEquals(successCount.get(),threadNum * 100);
    }


}
