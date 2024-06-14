package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcStatus;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author: Lee
 */
public class SelfAdaptionLoadBalance extends  AbstractLoadBalance{

    public static final String NAME = "selfadaption";

    private final int SLIDE_PERIOD = 10000;

    private AtomicBoolean onResetSlideWindow = new AtomicBoolean(false);

    private ConcurrentMap<RpcStatus,SlideWindowCalculat> map = new ConcurrentHashMap<RpcStatus,SlideWindowCalculat>();


    private volatile  long lastUpdateTime = System.currentTimeMillis();

    private ExecutorService executorService;

    public SelfAdaptionLoadBalance(){
        this.executorService = Executors.newCachedThreadPool(new NamedThreadFactory("Dubbo-framework-shared-handler",true));
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();

        long leastFailed = Long.MAX_VALUE;

        int leastFaiedCount = 0;

        /**
         *  the index of invokers having the same estimated shortest response time
         */
        int[] leastFailedIndexes = new int[length];
        /**
         * 每个服务提供者的权重
         */
        int[] weights = new int[length];

        int totalWeight = 0;

        int firstWeight = 0;

        boolean sameWeight = true;

        for (int i = 0;i<length;i++){
            Invoker<T> invoker = invokers.get(i);
            RpcStatus rpcStatus = RpcStatus.getStatus(invoker.getUrl(),invocation.getMethodName());
            SlideWindowCalculat slideWindowCalculat = map.computeIfAbsent(rpcStatus,SlideWindowCalculat::new);

            long estimateFailed = slideWindowCalculat.getEstimateFailGrade();

            int afterWarmup = getWeight(invoker,invocation);
            weights[i] = afterWarmup;

            if (estimateFailed  < leastFailed && afterWarmup > 0){

                leastFailed = estimateFailed;
                leastFaiedCount = 1;
                leastFailedIndexes[0] = i;
                totalWeight = afterWarmup;
                firstWeight = afterWarmup;
                sameWeight = true;

            }else if (estimateFailed == leastFailed){

                leastFailedIndexes[leastFaiedCount++] = i;
                totalWeight += afterWarmup;
                if (sameWeight && i >0 && afterWarmup != firstWeight){
                    sameWeight = false;
                }
            }

        }//end for

        if (System.currentTimeMillis() - this.lastUpdateTime > (long) this.SLIDE_PERIOD && this.onResetSlideWindow.compareAndSet(false,true)){
            this.executorService.execute(() ->{
                this.map.values().forEach(SlideWindowCalculat::reset);
                this.lastUpdateTime = System.currentTimeMillis();
                this.onResetSlideWindow.set(false);
            });
        }

        /**
         * 最优解只有一个的情况
         */
        if (leastFaiedCount == 1){
            return invokers.get(leastFailedIndexes[0]);
        }

        //最优解不止1个
        if (!sameWeight && totalWeight > 0){
            int offsetWeight = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int i = 0; i < leastFaiedCount;i++){
                int shortestIndex  = leastFailedIndexes[i];
                offsetWeight -= weights[shortestIndex];
                if (offsetWeight < 0 ){
                    return  invokers.get(shortestIndex);
                }
            }
        }

        /**
         * 最优解不止1个，且权重相同，最随机选择，兜底；
         */
        return invokers.get(leastFailedIndexes[ThreadLocalRandom.current().nextInt(leastFaiedCount)]);
    }

    protected static class SlideWindowCalculat{

        private long failedOffset;
        private long  failedElapasedOffset;

        private RpcStatus rpcStatus;

        public SlideWindowCalculat(RpcStatus rpcStatus){
            this.rpcStatus = rpcStatus;
            this.failedOffset = 0l;
            this.failedElapasedOffset = 0l;

        }

        public void reset(){
            this.failedOffset = this.rpcStatus.getFailed();
            this.failedElapasedOffset = this.rpcStatus.getFailedElapsed();
        }


        private long getFailAverageElapsed(){
            long failed = this.rpcStatus.getFailed() - this.failedOffset;
            return  failed == 0l ? 0l :(this.rpcStatus.getAverageElapsed() - this.failedElapasedOffset) / failed;
        }

        public long getEstimateFailGrade(){

            int active = this.rpcStatus.getActive() + 1;
            return  this.getFailAverageElapsed() * (long)active;
        }

    }
}
