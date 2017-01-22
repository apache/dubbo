package com.alibaba.dubbo.rpc.protocol.websocket;

import com.alibaba.dubbo.oauth2.property.TokenDetails;
import com.alibaba.dubbo.oauth2.support.OAuth2Service;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by wuyu on 2017/1/17.
 */
public class WebSocketClientPooledObjectFactory extends BasePooledObjectFactory<Socket> {

    private IO.Options options = new IO.Options();

    private String host;

    private String namespace;

    private boolean oAuth2;

    private OAuth2Service oAuth2Service = OAuth2Service.getInstance();

    public WebSocketClientPooledObjectFactory(String host, int timeout, boolean oAuth2) {
        options.transports = new String[]{"websocket"};
        options.reconnection = false;
        options.timeout = timeout;
        this.host = host;
        this.oAuth2 = oAuth2;
    }


    @Override
    public Socket create() throws Exception {
        Socket socket = IO.socket(this.host, options);
        // Called upon transport creation.
        socket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport) args[0];

                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>) args[0];
                        // modify request headers
                        if (oAuth2) {
                            TokenDetails tokenDetails = oAuth2Service.getTokenDetails();
                            headers.put("Authorization", Collections.singletonList("Bearer " + tokenDetails.getAccessToken()));
                        }

                    }
                });
            }
        });
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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
