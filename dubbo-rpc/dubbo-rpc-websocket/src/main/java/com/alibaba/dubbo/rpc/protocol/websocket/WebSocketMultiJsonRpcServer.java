package com.alibaba.dubbo.rpc.protocol.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by wuyu on 2017/1/21.
 */
public class WebSocketMultiJsonRpcServer extends JsonRpcBasicServer {

    private static final Logger LOGGER = Logger.getLogger(JsonRpcBasicServer.class.getName());

    private ObjectMapper mapper;

    private Map<String, WebSocketJsonRpcServer> serverMap = new ConcurrentHashMap<>();


    public WebSocketMultiJsonRpcServer(ObjectMapper mapper) {
        super(mapper, new BroadcastMessage());
        this.mapper = mapper;
    }

    public WebSocketMultiJsonRpcServer() {
        super(new BroadcastMessage());
        this.mapper = new ObjectMapper();
    }


    @Override
    public int handleObject(ObjectNode node, OutputStream ops) throws IOException {
        JsonNode jsonNode = node.get("method");
        if (jsonNode != null) {
            String method = jsonNode.asText();
            int lastIndexOf = method.lastIndexOf(".");
            if (lastIndexOf != -1) {
                String serviceName = method.substring(0, lastIndexOf);
                WebSocketJsonRpcServer webSocketJsonRpcServer = serverMap.get(serviceName);
                if (webSocketJsonRpcServer != null) {
                    node.put("method", method.substring(lastIndexOf + 1));
                    return webSocketJsonRpcServer.handleObject(node, ops);
                }
            }
        }
        return super.handleObject(node, ops);
    }

    public void addService(String name, WebSocketJsonRpcServer webSocketJsonRpcServer) {
        serverMap.put(name, webSocketJsonRpcServer);
    }

    public void removeService(String name) {
        serverMap.remove(name);
    }


}
