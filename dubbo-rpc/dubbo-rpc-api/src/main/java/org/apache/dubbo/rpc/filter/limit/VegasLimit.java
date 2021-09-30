package org.apache.dubbo.rpc.filter.limit;

import org.apache.dubbo.rpc.filter.function.Log10RootFunction;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class VegasLimit extends AbstractLimit{
    //func
    private static final Function<Integer,Integer> Log10Func = Log10RootFunction.create(0);
    private static final Function<Integer,Integer> alphaFunc = (limit) -> 3 * Log10Func.apply(limit.intValue());
    private static final Function<Integer,Integer> betaFunc =  (limit) -> 6 * Log10Func.apply(limit.intValue());
    private static final Function<Integer,Integer> thresholdFunc = (limit) -> Log10Func.apply(limit.intValue());
    private static final Function<Integer,Integer> increaseFunc = (limit) -> limit + Log10Func.apply(limit.intValue());
    private static final Function<Integer,Integer> decreaseFunc = (limit) -> limit - Log10Func.apply(limit.intValue());

    //const
    private static final int maxConcurrency = 1000;
    private static final int initLimit = 100;
    private static final int probeLimit = 50;

    //status
    private AtomicInteger probeCounter = new AtomicInteger();
    private AtomicLong rttAverage = new AtomicLong();

    //lock
    private ReentrantLock lock = new ReentrantLock();


    public VegasLimit(){
        super(initLimit);
    }

    @Override
    protected int doUpdate(long rtt, int inflight, boolean isDrop) {
        //judge should probe
        int probeCounter = getProbeCounter();
        if (shouldProbe(probeCounter)){
            rttAverage.set(rtt);
            return estimatedLimit.get();
        }
        //update rttAverage
        if (rttAverage.get()==0 || (rtt <= rttAverage.get()*1.5 && rtt >= rttAverage.get()*0.3)){
            double newRttAverage = rttAverage.get()*probeCounter/(probeCounter+1)+rtt/(probeCounter+1);
            rttAverage.set((int)newRttAverage);
        }
        return updateEstimatedLimit(rtt,inflight,isDrop);
    }

    private int updateEstimatedLimit(long rtt,int inflight,boolean isDrop){
        int queueSize = (int) Math.ceil(estimatedLimit.get() * ( 1 - (double)rttAverage.get()/rtt));
        int newLimit = estimatedLimit.get();
        if (isDrop){
            newLimit = decreaseFunc.apply(estimatedLimit.get());
        }else if (inflight*2 < estimatedLimit.get()){
            return estimatedLimit.get();
        }else{
            int alpha = alphaFunc.apply(estimatedLimit.get());
            int beta = betaFunc.apply(estimatedLimit.get());
            int threshold = thresholdFunc.apply(estimatedLimit.get());
            if (queueSize <= threshold){
                newLimit = estimatedLimit.get() + beta;
            }else if (queueSize < alpha){
                newLimit = increaseFunc.apply(estimatedLimit.get());
            }else if (queueSize > beta){
                newLimit = decreaseFunc.apply(estimatedLimit.get());
            }
        }
        newLimit = Math.max(1,Math.min(newLimit,maxConcurrency));
        estimatedLimit.set(newLimit);
        return newLimit;
    }

    private boolean shouldProbe(int probeCount){
        return probeCount >= probeLimit-1;
    }

    private int getProbeCounter(){
        int count;
        try{
            lock.lock();
            count = probeCounter.incrementAndGet();
            count = count%probeLimit;
            probeCounter.set(count);
        }
        finally{
            lock.unlock();
        }
        return count;
    }
}
