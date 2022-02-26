package org.apache.dubbo.common.threadpool.support.limited;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Can completely solve the OOM problem caused by {@link java.util.concurrent.LinkedBlockingQueue}.
 */
public class MemoryLimitedLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

    private static final long serialVersionUID = 1374792064759926198L;

    private final MemoryLimiter memoryLimiter;

    public MemoryLimitedLinkedBlockingQueue(Instrumentation inst) {
        this(Integer.MAX_VALUE, inst);
    }

    public MemoryLimitedLinkedBlockingQueue(long memoryLimit, Instrumentation inst) {
        super(Integer.MAX_VALUE);
        this.memoryLimiter = new MemoryLimiter(memoryLimit, inst);
    }

    public MemoryLimitedLinkedBlockingQueue(Collection<? extends E> c, long memoryLimit, Instrumentation inst) {
        super(c);
        this.memoryLimiter = new MemoryLimiter(memoryLimit, inst);
    }

    public void setMemoryLimit(long memoryLimit) {
        memoryLimiter.setMemoryLimit(memoryLimit);
    }

    public long getMemoryLimit() {
        return memoryLimiter.getMemoryLimit();
    }

    public long getCurrentMemory() {
        return memoryLimiter.getCurrentMemory();
    }

    public long getCurrentRemainMemory() {
        return memoryLimiter.getCurrentRemainMemory();
    }

    @Override
    public void put(E e) throws InterruptedException {
        memoryLimiter.acquireInterruptibly(e);
        super.put(e);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        return memoryLimiter.acquire(e, timeout, unit) && super.offer(e, timeout, unit);
    }

    @Override
    public boolean offer(E e) {
        return memoryLimiter.acquire(e) && super.offer(e);
    }

    @Override
    public E take() throws InterruptedException {
        final E e = super.take();
        memoryLimiter.releaseInterruptibly(e);
        return e;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        final E e = super.poll(timeout, unit);
        memoryLimiter.releaseInterruptibly(e, timeout, unit);
        return e;
    }

    @Override
    public E poll() {
        final E e = super.poll();
        memoryLimiter.release(e);
        return e;
    }

    @Override
    public boolean remove(Object o) {
        final boolean success = super.remove(o);
        if (success) {
            memoryLimiter.release(o);
        }
        return success;
    }

    @Override
    public void clear() {
        super.clear();
        memoryLimiter.clear();
    }
}
