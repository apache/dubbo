package org.apache.dubbo.rpc.filter.limit;

public interface Limit {
    int getLimit();

    void updateLimit(long rtt,int inflight,boolean isDrop);
}
