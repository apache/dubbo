package com.alibaba.dubbo.rpc.protocol.springmvc.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcMultiServer;
import com.googlecode.jsonrpc4j.JsonRpcService;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/2/13.
 */
public class WebSocketJsonRpcHandler extends TextWebSocketHandler implements ApplicationContextAware {

    private Map<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    private JsonRpcMultiServer jsonRpcMultiServer = new JsonRpcMultiServer();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String id = (String) session.getAttributes().get("id");
        if (id != null) {
            WebSocketSession webSocketSession = sessionMap.get(id);
            if (webSocketSession != null) {
                webSocketSession.close();
            }
            sessionMap.put(id, session);
        }
        super.afterConnectionEstablished(session);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer payload = message.getPayload();
        byte[] bytes = payload.array();
        try {
            handleTextMessage(session, new TextMessage(bytes));
        } catch (Exception e) {
            super.handleBinaryMessage(session, message);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try (ByteArrayInputStream in = new ByteArrayInputStream(payload.getBytes()); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            jsonRpcMultiServer.handle(in, out);
            session.sendMessage(new TextMessage(out.toByteArray()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        remove(session);
        super.afterConnectionClosed(session, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        remove(session);
        super.handleTransportError(session, exception);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, JsonRpcMultiServer> jsonRpcMultiServerMap = applicationContext.getBeansOfType(JsonRpcMultiServer.class);
        if (jsonRpcMultiServerMap.size() >= 0) {
            jsonRpcMultiServer = jsonRpcMultiServerMap.values().iterator().next();
        }
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(JsonRpcService.class);
        for (Map.Entry<String, Object> entity : beansWithAnnotation.entrySet()) {
            String key = entity.getKey();
            Object bean = applicationContext.getBean(key);
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            jsonRpcMultiServer.addService(targetClass.getAnnotation(JsonRpcService.class).value(), bean, targetClass);
        }
    }


    private void remove(WebSocketSession webSocketSession) {
        String id = (String) webSocketSession.getAttributes().get("id");
        if (id != null) {
            sessionMap.remove(id);
            try {
                webSocketSession.close();
            } catch (Exception e) {

            }
        }
    }

    public WebSocketSession remove(String id) {
        return sessionMap.remove(id);
    }

    public WebSocketSession getWebSocketSessionById(String id) {
        return sessionMap.get(id);
    }

    public void addService(String key, Object handler, Class type) {
        jsonRpcMultiServer.addService(key, handler, type);
    }

    public void addService(Object handler, Class type) {
        jsonRpcMultiServer.addService(type.getName(), handler);
    }

    public Map<String, WebSocketSession> getSessionMap() {
        return Collections.unmodifiableMap(sessionMap);
    }

    public void sendBroadcast(String message) throws IOException {
        for (WebSocketSession webSocketSession : sessionMap.values()) {
            if (webSocketSession != null) {
                if (webSocketSession.isOpen()) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put("jsonrpc", 2.0);
                    objectNode.put("result", message);
                    webSocketSession.sendMessage(new TextMessage(objectNode.toString()));
                } else {
                    remove(webSocketSession);
                }
            }
        }
    }

    public boolean sendMessageToUser(String id, String message) throws IOException {
        WebSocketSession webSocketSession = sessionMap.get(id);
        if (webSocketSession != null) {
            if (webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(message));
                return true;
            } else {
                remove(webSocketSession);
                return false;
            }
        }
        return false;
    }

}
