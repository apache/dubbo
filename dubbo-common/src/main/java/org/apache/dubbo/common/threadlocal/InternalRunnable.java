package org.apache.dubbo.common.threadlocal;


/**
 * InternalRunnable
 * There is a risk of memory leak when using {@link InternalThreadLocal} without calling
 * {@link InternalThreadLocal#removeAll()}.
 * This design is learning from {@see io.netty.util.concurrent.FastThreadLocalRunnable} which is in Netty.
 */
public class InternalRunnable implements Runnable{
    private final Runnable runnable;

    public InternalRunnable(Runnable runnable){
        this.runnable=runnable;
    }

    /**
     * After the task execution is completed, it will call {@link InternalThreadLocal#removeAll()} to clear
     * unnecessary variables in the thread.
     */
    @Override
    public void run() {
        try{
            runnable.run();
        }finally {
            InternalThreadLocal.removeAll();
        }
    }

    /**
     * Wrap ordinary Runnable into {@link InternalThreadLocal}.
     */
    public static Runnable Wrap(Runnable runnable){
        return runnable instanceof InternalRunnable?runnable:new InternalRunnable(runnable);
    }
}
