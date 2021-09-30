package org.apache.dubbo.rpc.filter.limiter;

import java.util.Optional;

public class SimpleLimiter extends AbstractLimiter{

    public SimpleLimiter(){
        super();
    }

    @Override
    public Optional<Listener> acquire() {
        int limit = limitAlgorithm.getLimit();
        int flight = inflight.get();
        if (flight >= limit){
            return createRejectListener();
        }
        return Optional.of(createSuccessListener());
    }
}
