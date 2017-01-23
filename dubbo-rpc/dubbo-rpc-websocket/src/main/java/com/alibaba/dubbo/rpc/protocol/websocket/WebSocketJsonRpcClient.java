package com.alibaba.dubbo.rpc.protocol.websocket;

import com.alibaba.dubbo.rpc.RpcException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.googlecode.jsonrpc4j.*;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by wuyu on 2017/1/21.
 */
public class WebSocketJsonRpcClient extends JsonRpcClient {

    private static final Logger LOGGER = Logger.getLogger(JsonRpcClient.class.getName());

    private Semaphore semaphore = new Semaphore(1);

    private Object result;

    private Throwable exception;

    private ObjectMapper mapper;

    private int timeout = 1000;

    private RequestListener requestListener;

    private ExceptionResolver exceptionResolver = DefaultExceptionResolver.INSTANCE;

    public Object getResult() {

        try {
            semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            setException(e);
        }

        if (getException() != null) {
            throw new RpcException(getException());
        }
        return result;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
        release();
    }


    public WebSocketJsonRpcClient(int timeout, ObjectMapper objectMapper) {
        super(objectMapper);
        this.mapper = objectMapper;
        this.timeout = timeout;
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RpcException(e);
        }
    }

    public void setResult(Object result) {
        this.result = result;
        release();
    }

    private void release() {
        semaphore.release();
    }


    @Override
    public Object readResponse(Type returnType, InputStream ips, String id) throws Throwable {

        // get node iterator
        ReadContext ctx = ReadContext.getReadContext(ips, mapper);

        // read the response
        ctx.assertReadable();
        JsonNode response = ctx.nextValue();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "JSON-PRC Response: {0}", response.toString());
        }

        // bail on invalid response
        if (!response.isObject()) {
            throw new JsonRpcClientException(0, "Invalid JSON-RPC response", response);
        }
        ObjectNode jsonObject = ObjectNode.class.cast(response);

        if (id != null) {
            while (!jsonObject.has("id") ||
                    jsonObject.get("id") == null ||
                    !jsonObject.get("id").asText().equals(id)) {
                response = ctx.nextValue();

                if (!response.isObject()) {
                    throw new JsonRpcClientException(0, "Invalid JSON-RPC response", response);
                }
                jsonObject = ObjectNode.class.cast(response);
            }
        }

        // show to listener
        if (this.requestListener != null) {
            this.requestListener.onBeforeResponseProcessed(this, jsonObject);
        }

        // detect errors
        if (jsonObject.has("error")
                && jsonObject.get("error") != null
                && !jsonObject.get("error").isNull()) {

            // resolve and throw the exception
            if (exceptionResolver == null) {
                throw DefaultExceptionResolver.INSTANCE.resolveException(jsonObject);
            } else {
                throw exceptionResolver.resolveException(jsonObject);
            }
        }

        // convert it to a return object
        if (jsonObject.has("result")
                && !jsonObject.get("result").isNull()
                && jsonObject.get("result") != null) {
            if (returnType == null) {
                LOGGER.warning(
                        "Server returned result but returnType is null");
                return null;
            }

            JsonParser returnJsonParser = mapper.treeAsTokens(jsonObject.get("result"));
            JavaType returnJavaType = TypeFactory.defaultInstance().constructType(returnType);


            return mapper.readValue(returnJsonParser, returnJavaType);
        }

        // no return type
        return super.readResponse(returnType, ips, id);
    }
}
