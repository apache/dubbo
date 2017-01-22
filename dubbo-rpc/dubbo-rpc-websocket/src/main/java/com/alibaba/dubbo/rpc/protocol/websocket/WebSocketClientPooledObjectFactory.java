package com.alibaba.dubbo.rpc.protocol.websocket;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * Created by wuyu on 2017/1/17.
 */
public class WebSocketClientPooledObjectFactory extends BasePooledObjectFactory<Socket> {

    private IO.Options options = new IO.Options();

    private String host;

    private int port;

    private String namespace;

    public WebSocketClientPooledObjectFactory(String host, String namespace, int port, int timeout) {
        options.transports = new String[]{"websocket"};
        options.reconnection =false;
        options.timeout = timeout;
        this.host = host;
        this.port = port;
        this.namespace = namespace;
    }


    @Override
    public Socket create() throws Exception {
        Socket socket = IO.socket(this.host + ":" + this.port + "/" + namespace, options);
        socket.connect();
        return socket;
    }

    @Override
    public PooledObject<Socket> wrap(Socket obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public boolean validateObject(PooledObject<Socket> p) {
        return p.getObject().connected();
    }

    @Override
    public void destroyObject(PooledObject<Socket> p) throws Exception {
        p.getObject().close();
    }

    @Override
    public void activateObject(PooledObject<Socket> p) throws Exception {
        p.getObject().open();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
