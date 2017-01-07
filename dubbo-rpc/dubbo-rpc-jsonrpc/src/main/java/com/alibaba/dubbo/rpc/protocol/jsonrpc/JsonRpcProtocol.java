package com.alibaba.dubbo.rpc.protocol.jsonrpc;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.googlecode.jsonrpc4j.HttpException;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.googlecode.jsonrpc4j.JsonRpcServer;
import com.googlecode.jsonrpc4j.spring.JsonProxyFactoryBean;
import org.springframework.remoting.RemoteAccessException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuwen on 15/4/1.
 */
public class JsonRpcProtocol extends AbstractProxyProtocol {

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<>();

    private final Map<String, JsonRpcServer> skeletonMap = new ConcurrentHashMap<>();

    private HttpBinder httpBinder;

    public JsonRpcProtocol() {
        super(HttpException.class, JsonRpcClientException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    public int getDefaultPort() {
        return 80;
    }

    private class InternalHandler implements HttpHandler {

        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            String uri = request.getRequestURI();
            JsonRpcServer skeleton = skeletonMap.get(uri);
            if (! request.getMethod().equalsIgnoreCase("POST")) {
                response.setStatus(500);
            } else {
                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
                try {
                    skeleton.handle(request.getInputStream(), response.getOutputStream());
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            }
        }

    }

    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String addr = url.getIp() + ":" + url.getPort();
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            server = httpBinder.bind(url, new InternalHandler());
            serverMap.put(addr, server);
        }
        final String path = url.getAbsolutePath();
        JsonRpcServer skeleton = new JsonRpcServer(impl, type);
        skeletonMap.put(path, skeleton);
        return new Runnable() {
            public void run() {
                skeletonMap.remove(path);
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected <T> T doRefer(final Class<T> serviceType, URL url) throws RpcException {
        JsonProxyFactoryBean jsonProxyFactoryBean = new JsonProxyFactoryBean();
        jsonProxyFactoryBean.setServiceUrl(url.setProtocol("http").toIdentityString());
        jsonProxyFactoryBean.setServiceInterface(serviceType);

        jsonProxyFactoryBean.afterPropertiesSet();
        return (T) jsonProxyFactoryBean.getObject();
    }

    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null) {
            Class<?> cls = e.getClass();
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            } else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }

    public void destroy() {
        super.destroy();
        for (String key : new ArrayList<>(serverMap.keySet())) {
            HttpServer server = serverMap.remove(key);
            if (server != null) {
                try {
                    if (logger.isInfoEnabled()) {
                        logger.info("Close jsonrpc server " + server.getUrl());
                    }
                    server.close();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            }
        }
    }

}