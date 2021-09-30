package org.apache.dubbo.rpc.filter.limiter;

import org.apache.dubbo.rpc.filter.limit.Limit;
import org.apache.dubbo.rpc.filter.limit.VegasLimit;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public abstract class AbstractLimiter implements Limiter{
    private static final Supplier<Long> clock = System::nanoTime;
    protected Limit limitAlgorithm;
    protected AtomicInteger inflight;

    public AbstractLimiter(){
        this.limitAlgorithm = new VegasLimit();
        this.inflight = new AtomicInteger(0);
    }

    public int getLimit(){
        return limitAlgorithm.getLimit();
    }

    public int getInflight(){return inflight.get();}

    public int getRemain(){
        return limitAlgorithm.getLimit() - inflight.get();
    }

    protected Listener createSuccessListener(){
        final long startTime = clock.get();
        final int flight = this.inflight.incrementAndGet();
        return new Listener(){
            @Override
            public void onSuccess() {
                limitAlgorithm.updateLimit(clock.get() - startTime,flight,false);
                inflight.decrementAndGet();
            }

            @Override
            public void onDroped() {
                limitAlgorithm.updateLimit(clock.get() - startTime,flight,true);
                inflight.decrementAndGet();
            }
        };
    }

    protected Optional<Listener> createRejectListener(){
        return Optional.empty();
    }
}
