package com.alibaba.dubbo.rpc.protocol.websocket;

import com.alibaba.dubbo.rpc.RpcException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import rx.Observable;
import rx.Subscriber;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by wuyu on 2017/1/21.
 */
public class WebSocketJsonRpcServer extends JsonRpcBasicServer {

    private static final Logger LOGGER = Logger.getLogger(JsonRpcBasicServer.class.getName());

    private ObjectMapper mapper;

    private final int timeout;

    public WebSocketJsonRpcServer(ObjectMapper mapper, Object handler, Class<?> remoteInterface, int timeout) {
        super(mapper, handler, remoteInterface);
        this.mapper = mapper;
        this.timeout = timeout;
    }

    @Override
    protected JsonNode invoke(Object target, Method m, List<JsonNode> params) throws IOException, IllegalAccessException, InvocationTargetException {
        // debug log
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Invoking method: " + m.getName());
        }

        // convert the parameters
        Object[] convertedParams = new Object[params.size()];
        Type[] parameterTypes = m.getGenericParameterTypes();

        for (int i = 0; i < parameterTypes.length; i++) {
            JsonParser paramJsonParser = mapper.treeAsTokens(params.get(i));
            JavaType paramJavaType = TypeFactory.defaultInstance().constructType(parameterTypes[i]);
            convertedParams[i] = mapper.readValue(paramJsonParser, paramJavaType);
        }

        // invoke the method
        Object result = m.invoke(target, convertedParams);
        Class<?> genericReturnClazz = m.getReturnType();

        if (Observable.class.isAssignableFrom(genericReturnClazz)) {
            final List<Object> list = new ArrayList<>();
            final Semaphore semaphore = new Semaphore(1);

            Observable rxResult = (Observable) result;
            rxResult.subscribe(new Subscriber() {
                @Override
                public void onCompleted() {
                    semaphore.release();
                }

                @Override
                public void onError(Throwable e) {
                    throw new RpcException(e);
                }

                @Override
                public void onNext(Object o) {
                    list.add(o);
                }
            });
            try {
                semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RpcException(e);
            }
            return mapper.valueToTree(list);
        } else if (Future.class.isAssignableFrom(genericReturnClazz)) {
            Future future = (Future) result;
            try {
                return mapper.valueToTree(future.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        return (m.getGenericReturnType() != null) ? mapper.valueToTree(result) : null;
    }
}
