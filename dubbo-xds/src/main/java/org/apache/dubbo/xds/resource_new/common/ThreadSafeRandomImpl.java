package org.apache.dubbo.xds.resource_new.common;

import java.util.concurrent.ThreadLocalRandom;

public final class ThreadSafeRandomImpl implements ThreadSafeRandom {

    public static final ThreadSafeRandom instance = new ThreadSafeRandomImpl();

    private ThreadSafeRandomImpl() {}

    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current()
                .nextInt(bound);
    }

    @Override
    public long nextLong() {
        return ThreadLocalRandom.current()
                .nextLong();
    }

    @Override
    public long nextLong(long bound) {
        return ThreadLocalRandom.current()
                .nextLong(bound);
    }
}
