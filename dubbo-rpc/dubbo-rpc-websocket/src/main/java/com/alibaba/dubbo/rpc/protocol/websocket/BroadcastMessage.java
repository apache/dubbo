package com.alibaba.dubbo.rpc.protocol.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by wuyu on 2017/1/26.
 */
public class BroadcastMessage {

    private Object result;

    private String jsonrpc = "2.0";

    private ObjectMapper objectMapper = new ObjectMapper();

    public BroadcastMessage(Object result) {
        this.result = result;
    }

    public BroadcastMessage() {
    }

    public static BroadcastMessage newBuilder() {
        return new BroadcastMessage();
    }

    public String build() {
        return this.toMessage();
    }

    public String toMessage() {
        StringWriter stringWriter = new StringWriter();
        try {
            objectMapper.writeValue(stringWriter, this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stringWriter.close();
            } catch (IOException e) {

            }
        }
        return stringWriter.toString();
    }

    @Override
    public String toString() {
        return toMessage();
    }

    public Object getResult() {
        return result;
    }

    public BroadcastMessage setResult(Object result) {
        this.result = result;
        return this;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public BroadcastMessage setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
        return this;
    }
}
