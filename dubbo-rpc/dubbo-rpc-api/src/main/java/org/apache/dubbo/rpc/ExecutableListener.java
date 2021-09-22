package org.apache.dubbo.rpc;

import java.util.concurrent.Executor;

/**
 * @author earthchen
 * @date 2021/9/20
 **/
public class ExecutableListener implements Runnable {

    private final Executor executor;
    private final CancellationListener listener;
    private final CancellableContext context;

    public ExecutableListener(Executor executor, CancellationListener listener, CancellableContext context) {
        this.executor = executor;
        this.listener = listener;
        this.context = context;
    }

    public void deliver() {
        try {
            executor.execute(this);
        } catch (Throwable t) {
//            log.log(Level.INFO, "Exception notifying context listener", t);
        }
    }

    public Executor getExecutor() {
        return executor;
    }

    public CancellationListener getListener() {
        return listener;
    }

    public RpcContext getContext() {
        return context;
    }

    @Override
    public void run() {
        listener.cancelled(context);
    }
}
