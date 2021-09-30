package org.apache.dubbo.rpc.filter.limit;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractLimit implements Limit{
    protected AtomicInteger estimatedLimit;

    public AbstractLimit(int limit){
        this.estimatedLimit = new AtomicInteger(limit);
    }

    @Override
    public int getLimit() {
        return this.estimatedLimit.get();
    }

    public void setLimit(int newLimit){
        this.estimatedLimit.set(newLimit);
    }

    @Override
    public void updateLimit(long rtt, int inflight, boolean isDrop) {
        setLimit(doUpdate(rtt,inflight,isDrop));
    }


    protected abstract int doUpdate(long rtt,int inflight,boolean isDrop);
}
