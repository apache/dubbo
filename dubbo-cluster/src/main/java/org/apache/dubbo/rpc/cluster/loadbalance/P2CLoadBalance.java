package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.adaptive.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class P2CLoadBalance implements LoadBalance {
    private static final ConcurrentHashMap<String, List<Node>> method2Nodes = new ConcurrentHashMap<>(32);
    private static final Supplier<Long> clock = System::nanoTime;
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        //update method2Nodes
        updatemethod2Nodes(invokers,invocation);
        //prepick
        Node[] prepick = prepick(invocation);
        //pick
        Invoker pick = pick(prepick);
        //set pickTime
        invocation.put("pickTime",clock.get());
        return pick;
    }

    private <T> void updatemethod2Nodes(List<Invoker<T>> invokers,Invocation invocation){
        //todo 注册中心的健康检查机制
        String methodName = invocation.getMethodName();
        if (method2Nodes.containsKey(methodName))return;
        try{
            lock.lock();
            method2Nodes.computeIfAbsent(methodName,k-> new ArrayList<>());
            List<Node> nodes = method2Nodes.get(methodName);
            invokers.forEach(ivk->{
                boolean exist = false;
                for (Node node : nodes) {
                    if (node.getInvoker()==ivk){
                        exist =  true;
                        break;
                    }
                }
                if (!exist){
                    nodes.add(new Node(ivk));
                }
            });
        }finally{
            lock.unlock();
        }
    }

    private Node[] prepick(Invocation invocation){
        String methodName = invocation.getMethodName();
        List<Node> nodes = method2Nodes.get(methodName);
        int size = nodes.size();
        Node[] prepick = new Node[2];
        for (int i = 0; i < 3; i++) {
            int first = ThreadLocalRandom.current().nextInt(10 * size);
            int second = ThreadLocalRandom.current().nextInt(10 * size-1);
            if (second >= first){
                second+=1;
            }
            prepick[0] = nodes.get(first%size);
            prepick[1] = nodes.get(second%size);
            if (prepick[0].isValid()||prepick[1].isValid()){
                break;
            }
        }
        return prepick;
    }

    private Invoker pick(Node[] nodes){
        Node fir = nodes[0];
        Node sec = nodes[1];
        //default choice
        Node pc = fir;
        Node upc = sec;
        if (sec.getRemain() > fir.getRemain()){
            pc = sec;
            upc = fir;
        }
        return pc.getInvoker();
    }

    public static void updateNodes(int remain,String methodName,String backupAddr,long rtt){
        List<Node> nodes = method2Nodes.get(methodName);
        for (Node node : nodes) {
            if (node.getAddress().equals(backupAddr)){
                node.updateState(remain,rtt);
            }
        }
    }
}
