package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PowerOfTwoChoiceWithLeastActive extends AbstractLoadBalance{
    public static final String NAME = "p2c";

    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation){
        int length = invokers.size();
        if(length == 1){
            return invokers.get(0);
        }
        if(length == 2){
            return leastActive(invokers.get(0),invokers.get(1),invocation);
        }
        int pos1 = ThreadLocalRandom.current().nextInt(length);
        int pos2 = ThreadLocalRandom.current().nextInt(length);
        while(pos1 == pos2){
            pos2 = ThreadLocalRandom.current().nextInt(length);
        }
        return leastActive(invokers.get(pos1),invokers.get(pos2),invocation);
    }

    private <T> Invoker<T> leastActive(Invoker<T> invoker1,Invoker<T> invoker2,Invocation invocation){
        int active1 = RpcStatus.getStatus(invoker1.getUrl(),invocation.getMethodName()).getActive();
        int active2 = RpcStatus.getStatus(invoker2.getUrl(),invocation.getMethodName()).getActive();
        if(active1 <= active2){
            return invoker1;
        }
        return invoker2;
    }
}
