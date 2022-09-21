package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdaptivePowerOfTwoChoice extends AbstractLoadBalance implements ScopeModelAware {
    public static final String NAME = "adaptiveP2C";

    private int slidePeriod = 30_000;

    private ExecutorService executorService;

    private volatile long lastUpdateTime = System.currentTimeMillis();

    private AtomicBoolean onResetSlideWindow = new AtomicBoolean(false);

    private ConcurrentHashMap<RpcStatus,SlideWindowData> methodMap = new ConcurrentHashMap<>();

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        slidePeriod = applicationModel.getModelEnvironment().getConfiguration().getInt(Constants.ADAPTIVE_POWER_OF_TWO_CHOICE_SLIDE_PERIOD,30_000);
        executorService = applicationModel.getFrameworkModel().getBeanFactory().getBean(FrameworkExecutorRepository.class).getSharedExecutor();
    }

    protected static class SlideWindowData{

        private RpcStatus rpcStatus;

        private long totalOffset;

        private long succeedOffset;

        private long totalElapsedOffset;

        private long succeedElapsedOffset;

        public SlideWindowData(RpcStatus rpcStatus){
            this.rpcStatus = rpcStatus;
            this.totalOffset = 0;
            this.totalElapsedOffset = 0;
            this.succeedElapsedOffset = 0;
            this.succeedOffset = 0;
        }

        public void reset(){
            this.totalOffset = rpcStatus.getTotal();
            this.succeedOffset = rpcStatus.getSucceeded();
            this.totalElapsedOffset = rpcStatus.getTotalElapsed();
            this.succeedElapsedOffset = rpcStatus.getSucceededElapsed();
        }

        public double getLatency(){
            if((this.rpcStatus.getSucceeded() - this.succeedOffset) == 0){
                return 0;
            }
            return (double) (this.rpcStatus.getSucceededElapsed() - this.succeedElapsedOffset)/(this.rpcStatus.getSucceeded() - this.succeedOffset);
        }

        public long getAccept(){
            return (this.rpcStatus.getSucceeded() - this.succeedOffset);
        }

        private long getRequest(){
            return (this.rpcStatus.getTotal() - this.totalOffset);
        }

    }

    private double getNormlize(double x){
        return x/(1 + x);
    }

    public <T> double getWeight(Invoker<T> invoker,Invocation invocation,double averageLatency){
        RpcStatus rpcStatus = RpcStatus.getStatus(invoker.getUrl(),invocation.getMethodName());
        SlideWindowData slideWindowData = methodMap.get(rpcStatus);
        double latency = (1 + averageLatency)/(1 + slideWindowData.getLatency());

        return (1 + slideWindowData.getAccept())/(1 + slideWindowData.getRequest()) * getNormlize(latency);
    }

    private <T> Invoker<T> leastWeight(Invoker<T> invoker1,Invoker<T> invoker2,Invocation invocation,double averageLatency){
        double weight1 = getWeight(invoker1,invocation,averageLatency);
        double weight2 = getWeight(invoker2,invocation,averageLatency);
        if(weight1 >= weight2)
            return invoker1;
        return invoker2;
    }

    public <T> double getAverageLatency(List<Invoker<T>> invokers,Invocation invocation){
        double averageLatency = 0;
        double totalLatency = 0;
        int length = invokers.size();
        for(int i = 0; i < length; i++){
            RpcStatus rpcStatus = RpcStatus.getStatus(invokers.get(i).getUrl(),invocation.getMethodName());
            SlideWindowData slideWindowData = methodMap.computeIfAbsent(rpcStatus,SlideWindowData::new);
            totalLatency += slideWindowData.getLatency();
        }
        averageLatency = totalLatency/length;
        return averageLatency;
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();
        if(length == 1)
            return invokers.get(0);

        double averageLatency = 0;
        double totalLatency = 0;
        for(int i = 0; i < length; i++){
            RpcStatus rpcStatus = RpcStatus.getStatus(invokers.get(i).getUrl(),invocation.getMethodName());
            SlideWindowData slideWindowData = methodMap.computeIfAbsent(rpcStatus,SlideWindowData::new);
            totalLatency += slideWindowData.getLatency();
        }
        averageLatency = totalLatency/length;

        if(length == 2)
            return leastWeight(invokers.get(0),invokers.get(1),invocation,averageLatency);

        int pos1 = ThreadLocalRandom.current().nextInt(length);
        int pos2 = ThreadLocalRandom.current().nextInt(length);

        while(pos1 == pos2)
            pos2 = ThreadLocalRandom.current().nextInt(length);

        Invoker<T> result = leastWeight(invokers.get(pos1),invokers.get(pos2),invocation,averageLatency);

        if(System.currentTimeMillis() - lastUpdateTime > slidePeriod && onResetSlideWindow.compareAndSet(false,true)){
            executorService.execute(() -> {
                methodMap.values().forEach(SlideWindowData::reset);
                lastUpdateTime = System.currentTimeMillis();
                onResetSlideWindow.set(false);
            });
        }

        return result;
    }
}
