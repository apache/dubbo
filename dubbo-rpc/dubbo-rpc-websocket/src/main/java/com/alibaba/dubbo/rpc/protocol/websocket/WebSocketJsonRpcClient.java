package com.alibaba.dubbo.rpc.protocol.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.apache.commons.pool2.impl.GenericObjectPool;
import rx.Observable;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Created by wuyu on 2017/1/21.
 */
public class WebSocketJsonRpcClient extends JsonRpcClient implements Future {

    private static final Logger LOGGER = Logger.getLogger(JsonRpcClient.class.getName());

    private Object result;

    private ExecutionException exception;

    private boolean done;

    private final GenericObjectPool<Socket> pool;

    private final Method method;

    private Object[] args;

    private int timeout;

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = new ExecutionException(exception);
    }

    public WebSocketJsonRpcClient(GenericObjectPool<Socket> pool, Method method, Object[] args, int timeout) {
        super(new ObjectMapper());
        this.pool = pool;
        this.method = method;
        this.args = args;
        this.timeout = timeout;
    }

    public void call() throws Exception {
        ObjectNode request = createRequest(method.getName(), args);
        final Socket socket = pool.borrowObject(timeout);
        socket.once(Socket.EVENT_MESSAGE, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Object result = null;
                try {
                    Type returnType = method.getGenericReturnType();
                    Class returnClazz = method.getReturnType();
                    if (Future.class.isAssignableFrom(returnClazz)) {
                        Type genericType = ((ParameterizedTypeImpl) returnType).getActualTypeArguments()[0];
                        result = readResponse(genericType, new ByteArrayInputStream(args[0].toString().getBytes("utf-8")));
                    } else if (Observable.class.isAssignableFrom(returnClazz)) {
                        Type genericType = ((ParameterizedTypeImpl) returnType).getActualTypeArguments()[0];
                        List<Object> list = new ArrayList<>();
                        JsonNode jsonnode = getObjectMapper().readValue(args[0].toString(), JsonNode.class);
                        JsonNode resultNode = jsonnode.get("result");
                        if (resultNode.isArray()) {
                            for (int i = 0; i < resultNode.size(); i++) {
                                Object ele = getObjectMapper().readValue(resultNode.get(i).toString(), (Class) genericType);
                                list.add(ele);
                            }
                            result = list;
                        }

                    } else {
                        result = readResponse(returnType, new ByteArrayInputStream(args[0].toString().getBytes("utf-8")));
                    }
                    onComplete(result);
                } catch (Throwable throwable) {
                    onError(throwable);
                } finally {
                    pool.returnObject(socket);
                }
            }
        });
        socket.send(request);
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public synchronized Object get() throws InterruptedException, ExecutionException {
        if (done) {
            return result;
        }
        this.wait();
        if (!done) {
            onError(new RuntimeException("Waiting server-side response timeout"));
        }
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    @Override
    public synchronized Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (done) {
            return result;
        }
        this.wait(timeout);
        if (!done) {
            onError(new RuntimeException("Waiting server-side response timeout"));
        }
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    public synchronized void onComplete(Object result) {
        this.result = result;
        done = true;
        this.notify();
    }

    public synchronized void onError(Throwable t) {
        exception = new ExecutionException(t);
        done = true;
        this.notify();
    }
}
