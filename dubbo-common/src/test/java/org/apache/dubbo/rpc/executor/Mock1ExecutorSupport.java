package org.apache.dubbo.rpc.executor;

import java.util.concurrent.Executor;

public class Mock1ExecutorSupport implements ExecutorSupport {
    @Override
    public Executor getExecutor(Object data) {
        return null;
    }
}
