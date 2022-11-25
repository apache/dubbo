package org.apache.dubbo.rpc.flowcontrol;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;



public class HeuristicSmoothingFlowControlTest extends FlowControlBaseTest {

    Long elapsed[];
    @BeforeEach
    public void setForHSTest(){
        elapsed = new Long[5];
        elapsed[0] = 1000L;
        elapsed[1] = 2000L;
        elapsed[2] = 3000L;
        elapsed[3] = 4000L;
        elapsed[4] = 5000L;

    }

    @Test
    public void testHeuristicSmoothingFlowControl(){
        HeuristicSmoothingFlowControl flowControl = new HeuristicSmoothingFlowControl();
        flowControl.setApplicationModel(ApplicationModel.defaultModel());
        int[] max = new int[5];
        for(int i = 0; i < 5; i++){
            //System.out.println(elapsed[i]);
            flowControl.serverMetricsCollector.end(elapsed[i]);
            max[i] = flowControl.getMaxConcurrency();
            System.out.println(max[i]);
        }

        for (int i = 0; i < 4; i++){
            Assertions.assertTrue(max[i] >= max[i+1]);
        }
    }


    @Test
    public void testHeuristicSmoothingFlowControlInMultiThread() throws Exception {
        HeuristicSmoothingFlowControl flowControl = new HeuristicSmoothingFlowControl();
        flowControl.setApplicationModel(ApplicationModel.defaultModel());

        int threadNum = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadNum);
        List<Thread>threadList = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadNum; i++){
            Thread thread = new Thread(() -> {
                try{
                    startLatch.await();
                    for(int j=0;j<5;j++){
                        flowControl.serverMetricsCollector.end(100L);
                        successCount.incrementAndGet();
                        System.out.println("epoch is " + j +" the max concurrency is " + flowControl.getMaxConcurrency());
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
        Assertions.assertEquals(successCount.get(),5 * threadNum);
    }

}
