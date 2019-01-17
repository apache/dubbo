package org.apache.dubbo.rpc.protocol.rsocket;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.AtomicPositiveInteger;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.buffer.ChannelBufferOutputStream;
import org.apache.dubbo.remoting.exchange.ExchangeClient;
import org.apache.dubbo.remoting.exchange.ResponseFuture;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcResult;
import org.apache.dubbo.rpc.SimpleAsyncRpcResult;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.support.RpcUtils;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sixie.xyn on 2019/1/2.
 */
public class RSocketInvoker<T> extends AbstractInvoker<T> {

    private final RSocket[] clients;

    private final AtomicPositiveInteger index = new AtomicPositiveInteger();

    private final String version;

    private final ReentrantLock destroyLock = new ReentrantLock();

    private final Set<Invoker<?>> invokers;

    public RSocketInvoker(Class<T> serviceType, URL url, RSocket[] clients, Set<Invoker<?>> invokers) {
        super(serviceType, url, new String[]{Constants.INTERFACE_KEY, Constants.GROUP_KEY, Constants.TOKEN_KEY, Constants.TIMEOUT_KEY});
        this.clients = clients;
        // get version.
        this.version = url.getParameter(Constants.VERSION_KEY, "0.0.0");
        this.invokers = invokers;
    }

    @Override
    protected Result doInvoke(final Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;
        final String methodName = RpcUtils.getMethodName(invocation);
        inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
        inv.setAttachment(Constants.VERSION_KEY, version);

        RSocket currentClient;
        if (clients.length == 1) {
            currentClient = clients[0];
        } else {
            currentClient = clients[index.getAndIncrement() % clients.length];
        }
        try {
            boolean isAsync = RpcUtils.isAsync(getUrl(), invocation);
            boolean isAsyncFuture = RpcUtils.isFutureReturnType(inv);
            boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);
            int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
            if (isOneway) {
//                boolean isSent = getUrl().getMethodParameter(methodName, Constants.SENT_KEY, false);
//                currentClient.send(inv, isSent);
//                RpcContext.getContext().setFuture(null);
//                return new RpcResult();
            } else if (isAsync) {
                //encode metadata and data
                byte[] metadataBytes = encodeMetadata(invocation);
                byte[] dataBytes = encodeData(invocation);
                Payload requestPayload = DefaultPayload.create(dataBytes, metadataBytes);

                //ResponseFuture future = currentClient.request(inv, timeout);
                Mono<Payload> responseMono = currentClient.requestResponse(requestPayload);

                // For compatibility
                FutureAdapter<Object> futureAdapter = new FutureAdapter<>(future);
                RpcContext.getContext().setFuture(futureAdapter);

                Result result;
                if (isAsyncFuture) {
                    // register resultCallback, sometimes we need the async result being processed by the filter chain.
                    result = new AsyncRpcResult(futureAdapter, futureAdapter.getResultFuture(), false);
                } else {
                    result = new SimpleAsyncRpcResult(futureAdapter, futureAdapter.getResultFuture(), false);
                }
                return result;
            } else {
                RpcContext.getContext().setFuture(null);

                return (Result) currentClient.request(inv, timeout).get();
            }
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        if (!super.isAvailable()) {
            return false;
        }
        for (RSocket client : clients) {
            if (client.availability() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        // in order to avoid closing a client multiple times, a counter is used in case of connection per jvm, every
        // time when client.close() is called, counter counts down once, and when counter reaches zero, client will be
        // closed.
        if (super.isDestroyed()) {
            return;
        } else {
            // double check to avoid dup close
            destroyLock.lock();
            try {
                if (super.isDestroyed()) {
                    return;
                }
                super.destroy();
                if (invokers != null) {
                    invokers.remove(this);
                }
                for (RSocket client : clients) {
                    try {
                        client.dispose();
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }

            } finally {
                destroyLock.unlock();
            }
        }
    }

    private byte[] encodeMetadata(Invocation invocation) throws IOException {
        Map<String, Object> metadataMap = new HashMap<String, Object>();
        metadataMap.put(RSocketConstants.VERSION_KEY, version);
        metadataMap.put(RSocketConstants.SERVICE_NAME_KEY, invocation.getAttachment(Constants.PATH_KEY));
        metadataMap.put(RSocketConstants.SERVICE_VERSION_KEY, invocation.getAttachment(Constants.VERSION_KEY));
        metadataMap.put(RSocketConstants.METHOD_NAME_KEY, invocation.getMethodName());
        metadataMap.put(RSocketConstants.PARAM_TYPE_KEY, ReflectUtils.getDesc(invocation.getParameterTypes()));
        return MetadataCodec.encodeMetadata(metadataMap);
    }

    private byte[] encodeData(Invocation invocation) throws IOException {
        ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
        Serialization serialization = CodecSupport.getSerialization(getUrl());
        ObjectOutput out = serialization.serialize(getUrl(), dataOutputStream);

        RpcInvocation inv = (RpcInvocation) invocation;
//        out.writeUTF(version);
//        out.writeUTF(inv.getAttachment(Constants.PATH_KEY));
//        out.writeUTF(inv.getAttachment(Constants.VERSION_KEY));
//        out.writeUTF(inv.getMethodName());
//        out.writeUTF(ReflectUtils.getDesc(inv.getParameterTypes()));

        Object[] args = inv.getArguments();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                out.writeObject(args[i]);
            }
        }
        out.writeObject(RpcUtils.getNecessaryAttachments(inv));

        //clean
        out.flushBuffer();
        if (out instanceof Cleanable) {
            ((Cleanable) out).cleanup();
        }
        return dataOutputStream.toByteArray();
    }


}
