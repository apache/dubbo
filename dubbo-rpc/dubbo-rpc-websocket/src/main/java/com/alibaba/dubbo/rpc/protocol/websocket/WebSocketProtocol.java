package com.alibaba.dubbo.rpc.protocol.websocket;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.oauth2.property.UserDetails;
import com.alibaba.dubbo.oauth2.support.OAuth2Service;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.store.StoreFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaders;
import io.socket.client.Socket;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import rx.Observable;
import rx.Subscriber;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by wuyu on 2017/1/19.
 */
public class WebSocketProtocol extends AbstractProxyProtocol {

    private final static Map<String, SocketIOServer> serverMap = new ConcurrentHashMap<>();

    private final Map<String, WebSocketJsonRpcServer> serviceMap = new ConcurrentHashMap<>();

    private static final Map<String, GenericObjectPool<Socket>> poolMap = new ConcurrentHashMap<>();

    private final OAuth2Service oAuth2Service = OAuth2Service.getInstance();


    @Override
    public int getDefaultPort() {
        return 0;
    }

    @Override
    protected <T> Runnable doExport(final T impl, final Class<T> type, final URL url) throws RpcException {
        String host = url.getHost();
        int port = url.getPort();

        final String addr = url.getHost() + ":" + url.getPort();
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);

        SocketIOServer socketIOServer = serverMap.get(addr);
        if (socketIOServer == null) {
            Configuration config = new Configuration();
            if (url.getParameter("anyhost", false)) {
                config.setHostname("0.0.0.0");
            } else {
                config.setHostname(host);
            }
            config.setPort(port);
            config.setWorkerThreads(connections);
            config.setUpgradeTimeout(timeout);
            config.setWorkerThreads(connections);
            socketIOServer = new SocketIOServer(config);
            socketIOServer.start();
            //检测 spring中是否存在 其他session存储工厂，如果存在 将使用spring 存储工厂
            //see https://github.com/mrniko/netty-socketio/wiki/How-To:-create-a-cluster-of-netty-socketio-servers
            Map<String, StoreFactory> beansOfType = ServiceBean.getSpringContext().getBeansOfType(StoreFactory.class);
            if (beansOfType.size() > 0) {
                config.setStoreFactory(ServiceBean.getSpringContext().getBean(StoreFactory.class));
            }

            serverMap.put(addr, socketIOServer);

        }


        SocketIONamespace socketIONamespace = socketIOServer.addNamespace("/" + url.getServiceInterface());
        serviceMap.put(url.getServiceInterface(), new WebSocketJsonRpcServer(new ObjectMapper(), impl, type, timeout));


        socketIONamespace.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                String filter = url.getParameter("service.filter", "");
                HandshakeData handshakeData = client.getHandshakeData();
                String error = "{\"status\":\"200\",\"OK\"}";

                try {
                    //判断是否以用户名密码验证方式
                    if (url.getUsername() != null && url.getPassword() != null) {
                        String username = handshakeData.getSingleUrlParam("username");
                        String password = handshakeData.getSingleUrlParam("password");
                        //如果验证不通过, 拒绝连接
                        if (!url.getUsername().equalsIgnoreCase(username) || !url.getPassword().equalsIgnoreCase(password)) {
                            error = "{\"status\":\"401\",\"error\":\"unauthorized\",\"error_description\":\"username or password e error!\"}";
                            throw new RpcException(error);
                        }
                        RpcContext.getContext().getAttachments().put("principal", username);
                        //判断是否存在Token，并且未开启oAuth2Filter
                    } else if (url.getParameter("token") != null && !filter.contains("oAuth2Filter")) {
                        String token = handshakeData.getSingleUrlParam("token");
                        if (!url.getParameter("token", "").equalsIgnoreCase(token)) {
                            error = "{\"status\":\"401\",\"error\":\"unauthorized\",\"error_description\":\"token " + token + " error!\"}";
                        }
                        RpcContext.getContext().getAttachments().put("principal", token);
                    } else if (filter.contains("oAuth2Filter")) {

                        //判断是否开启oauth2 如果开启，将启用OAuth2
                        String access_token = client.getHandshakeData().getSingleUrlParam("access_token");
                        if (access_token == null) {
                            HttpHeaders httpHeaders = client.getHandshakeData().getHttpHeaders();
                            String authorization = httpHeaders.get("Authorization");
                            if (authorization != null) {
                                access_token = authorization.replace("Bearer", "").trim();
                            }
                        }

                        if (access_token != null) {
                            UserDetails userInfo = oAuth2Service.getUserInfo(access_token);
                            String token = url.getParameter("token", "");
                            boolean flag = false;
                            for (String role : token.split(",")) {
                                if (userInfo.getAuthorities().contains(role.trim())) {
                                    flag = true;
                                    break;
                                }
                            }

                            if (!flag) {
                                error = "{\"status\":\"401\",\"error\":\"unauthorized\",\"error_description\":\"" + access_token + "access_token  not has role!\"}";
                                throw new RpcException(error);
                            }
                            RpcContext.getContext().getAttachments().put("principal", userInfo.getPrincipal());
                            RpcContext.getContext().getAttachments().put("access_token", access_token);
                        } else {
                            error = "{\"status\":\"401\",\"error\":\"unauthorized\",\"error_description\":\"access_token  not has role!\"}";
                            throw new RpcException(error);
                        }
                    }
                } catch (Exception e) {
                    client.sendEvent("auth", error);
                    client.disconnect();
                    logger.warn(error);
                }

                InetSocketAddress addr = (InetSocketAddress) client.getRemoteAddress();
                RpcContext.getContext().setRemoteAddress(addr);
            }
        });


        socketIONamespace.addEventListener(Socket.EVENT_MESSAGE, String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackSender) throws Exception {
                String namespace = client.getNamespace().getName().substring(1, client.getNamespace().getName().length());
                WebSocketJsonRpcServer jsonRpcMultiServer = serviceMap.get(namespace);
                ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes("utf-8"));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    jsonRpcMultiServer.handle(in, out);

                    byte[] bytes = out.toByteArray();
                    client.sendEvent(Socket.EVENT_MESSAGE, new String(bytes, 0, bytes.length, "utf-8"));
                } finally {
                    in.close();
                    out.close();
                }
            }
        });


        return new Runnable() {
            @Override
            public void run() {
                serverMap.get(addr).removeNamespace(url.getServiceInterface());
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, final URL url) throws RpcException {
        final int port = url.getPort();
        final int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        final int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
        boolean oauth2 = false;
        String host = "http://" + url.getHost() + ":" + port + "/" + url.getServiceInterface();
        if (url.getUsername() != null && url.getPassword() != null) {
            host = host + "?username=" + url.getUsername() + "&password=" + url.getPassword();
        } else if (url.getParameter("token") != null && !url.getParameter("reference.filter").contains("oAuth2Filter")) {
            host = host + "?token=" + url.getParameter("token");
        } else if (url.getParameter("reference.filter", "").contains("oAuth2Filter")) {
            oauth2 = true;
        }

        final String addr = url.getHost() + ":" + url.getPort();
        if (poolMap.get(addr) == null) {

            final WebSocketClientPooledObjectFactory factory = new WebSocketClientPooledObjectFactory(host, timeout, oauth2);
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(connections);
            config.setMaxIdle(5);
            config.setBlockWhenExhausted(false);
            config.setTestOnReturn(true);
            config.setMaxWaitMillis(timeout);
            config.setTestWhileIdle(true);
            GenericObjectPool<Socket> pool = new GenericObjectPool<>(factory, config);
            poolMap.put(addr, pool);
        }
        return (T) Proxy.newProxyInstance(WebSocketProtocol.class.getClassLoader(), new Class[]{type}, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
                final WebSocketJsonRpcClient jsonRpcClient = new WebSocketJsonRpcClient(poolMap.get(addr), method, args, timeout);
                Class returnClazz = method.getReturnType();
                jsonRpcClient.call();
                if (Future.class.isAssignableFrom(returnClazz)) {
                    return jsonRpcClient;
                } else if (Observable.class.isAssignableFrom(returnClazz)) {
                    return Observable.create(new Observable.OnSubscribe<Object>() {
                        @Override
                        public void call(Subscriber<? super Object> subscriber) {
                            try {
                                List result = (List) jsonRpcClient.get();
                                for (Object obj : result) {
                                    subscriber.onNext(obj);
                                }
                                subscriber.onCompleted();
                            } catch (Throwable e) {
                                subscriber.onError(e);
                            }
                        }
                    });
                }
                return jsonRpcClient.get(timeout, TimeUnit.MILLISECONDS);
            }
        });
    }


    @Override
    public void destroy() {

        Collection<SocketIOServer> servers = serverMap.values();
        for (SocketIOServer server : servers) {
            try {
                server.stop();
            } catch (Exception e) {

            }
        }

        Collection<GenericObjectPool<Socket>> pools = poolMap.values();
        for (GenericObjectPool pool : pools) {
            try {
                pool.clear();
                pool.close();
            } catch (Exception e) {

            }
        }

    }


    public static List<SocketIONamespace> getSocketNamespace(Class clazz) {
        List<SocketIONamespace> socketIONamespaces = new ArrayList<>();
        for (SocketIOServer socketIOServer : serverMap.values()) {
            SocketIONamespace namespace = socketIOServer.getNamespace("/" + clazz.getName());
            socketIONamespaces.add(namespace);
        }
        return socketIONamespaces;
    }

    public static Map<String, GenericObjectPool<Socket>> getClientPool() {
        return Collections.unmodifiableMap(poolMap);
    }

}
