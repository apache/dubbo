package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.rpc.Invoker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ShortestResponseLoadBalanceTest extends LoadBalanceBaseTest{

    @Test
    public void testSelectByWeight() {
        int sumInvoker1 = 0;
        int sumInvoker2 = 0;
        int loop = 10000;

        ShortestResponseLoadBalance lb = new ShortestResponseLoadBalance();
        for (int i = 0; i < loop; i++) {
            Invoker selected = lb.select(weightInvokersSR, null, weightTestInvocation);

            if (selected.getUrl().getProtocol().equals("test1")) {
                sumInvoker1++;
            }

            if (selected.getUrl().getProtocol().equals("test2")) {
                sumInvoker2++;
            }
            // never select invoker5 because it's active is more than invoker1 and invoker2
            Assertions.assertTrue(!selected.getUrl().getProtocol().equals("test5"), "select is not the least active one");
        }

        // the sumInvoker1 : sumInvoker2 approximately equal to 1: 9
        System.out.println(sumInvoker1);
        System.out.println(sumInvoker2);

        Assertions.assertEquals(sumInvoker1 + sumInvoker2, loop, "select failed!");
    }
}
