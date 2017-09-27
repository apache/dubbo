package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.remoting.exchange.support.DefaultFuture;
import com.alibaba.dubbo.rpc.AsyncResult;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.google.common.util.concurrent.ExecutionList;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by zhaohui.yu
 * 15/10/14
 */
class AsyncRpcResult implements AsyncResult, Serializable {

    private final ExecutionList executionList = new ExecutionList();

    private ResponseFuture internal;

    AsyncRpcResult(ResponseFuture internal) {
        this.internal = internal;

        internal.setCallback(new ResponseCallback() {
            @Override
            public void done(Object response) {
                executionList.execute();
            }

            @Override
            public void caught(Throwable exception) {
                executionList.execute();
            }
        });
    }

    @Override
    public void addListener(final Runnable runnable, final Executor executor) {
        executionList.add(runnable, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (internal instanceof DefaultFuture) {
            ((DefaultFuture) internal).cancel();
        }
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return internal.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        try {
            return getWithException(internal.get());
        } catch (com.alibaba.dubbo.remoting.TimeoutException e) {
            throw new ExecutionException(new RpcException(RpcException.TIMEOUT_EXCEPTION, e.getMessage()));
        } catch (RemotingException e) {
            throw new ExecutionException(new RpcException(RpcException.NETWORK_EXCEPTION, e.getMessage()));
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return getWithException(internal.get((int) unit.toMillis(timeout)));
        } catch (com.alibaba.dubbo.remoting.TimeoutException e) {
            throw new TimeoutException(e.getMessage());
        } catch (RemotingException e) {
            throw new ExecutionException(new RpcException(RpcException.NETWORK_EXCEPTION, e.getMessage()));
        }
    }

    private Object getWithException(Object ret) throws ExecutionException {
        if (ret instanceof Result) {
            Result result = (Result) ret;
            if (result.hasException()) {
                throw new ExecutionException(result.getException());
            }
            return result.getValue();
        }
        return ret;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Throwable getException() {
        return null;
    }

    @Override
    public boolean hasException() {
        return false;
    }

    @Override
    public Object recreate() throws Throwable {
        return null;
    }

    @Override
    public Object getResult() {
        return null;
    }

    @Override
    public Map<String, String> getAttachments() {
        return null;
    }

    @Override
    public String getAttachment(String key) {
        return null;
    }

    @Override
    public String getAttachment(String key, String defaultValue) {
        return null;
    }
}
