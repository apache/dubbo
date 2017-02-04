package com.alibaba.dubbo.rpc.protocol.hprose;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import hprose.client.HproseHttpClient;
import hprose.server.HproseHttpService;
import hprose.server.HttpContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Created by wuyu on 2017/2/3.
 */
public class HproseProtocol extends AbstractProxyProtocol {

    private final Map<Integer, HproseHttpService> hproseHttpServiceMap = new ConcurrentHashMap<>();
    private final List<HproseHttpClient> hproseHttpClients = new ArrayList<>();
    private final Map<Integer, ServletConfig> servletConfigMap = new ConcurrentHashMap<>();


    private HttpBinder httpBinder;

    @Override
    public int getDefaultPort() {
        return 8080;
    }

    @Override
    protected <T> Runnable doExport(T impl, final Class<T> type, final URL url) throws RpcException {
        final int port = url.getPort();
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        HproseHttpService hproseHttpService = hproseHttpServiceMap.get(port);
        if (hproseHttpService == null) {
            hproseHttpService = new HproseHttpService();
            hproseHttpService.setTimeout(timeout);
            hproseHttpServiceMap.put(port, hproseHttpService);
            ServletContext servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
            ServletConfig servletConfig = new SimpleServletConfig(servletContext);
            servletConfigMap.put(port, servletConfig);
            httpBinder.bind(url, new HproseHttpHandler());
        }

        hproseHttpService.add(impl, type, type.getName());

        return new Runnable() {
            @Override
            public void run() {
                HproseHttpService hproseHttpService = hproseHttpServiceMap.get(port);
                if (hproseHttpService != null) {
                    hproseHttpService.remove(type.getName());
                }
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);

        int port = url.getPort();
        String schema = "http://";
        if (port == 443 || port == 8433) {
            schema = "https://";
        }
        String api = schema + url.getHost() + ":" + url.getPort() + getContextPath(url);

        final HproseHttpClient hproseHttpClient = new HproseHttpClient(api);
        HproseHttpClient.setThreadPool(Executors.newFixedThreadPool(connections));
        hproseHttpClient.setRetry(0);
        hproseHttpClient.setTimeout(timeout);
        hproseHttpClients.add(hproseHttpClient);
        return hproseHttpClient.useService(type, type.getName());
    }

    private class HproseHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            int serverPort = request.getServerPort();
            RpcContext.getContext().setRemoteAddress(request.getRemoteHost(), serverPort);
            ServletConfig servletConfig = servletConfigMap.get(serverPort);
            ServletContext servletContext = ServletManager.getInstance().getServletContext(serverPort);
            HproseHttpService hproseHttpService = hproseHttpServiceMap.get(serverPort);
            hproseHttpService.handle(new HttpContext(hproseHttpService, request, response, servletConfig, servletContext));
        }
    }

    private static class SimpleServletConfig implements ServletConfig {

        private final ServletContext servletContext;

        public SimpleServletConfig(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        public String getServletName() {
            return "DispatcherServlet";
        }

        public ServletContext getServletContext() {
            return servletContext;
        }

        public String getInitParameter(String s) {
            return null;
        }

        public Enumeration getInitParameterNames() {
            return new Enumeration() {
                public boolean hasMoreElements() {
                    return false;
                }

                public Object nextElement() {
                    return null;
                }
            };
        }
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    @Override
    public void destroy() {
        hproseHttpServiceMap.clear();
        servletConfigMap.clear();
        for (HproseHttpClient hproseHttpClient : hproseHttpClients) {
            try {
                hproseHttpClient.close();
            } catch (Exception e) {

            }
        }
    }

    private String getContextPath(URL url) {
        int pos = url.getPath().lastIndexOf("/");
        return pos > 0 ? url.getPath().substring(0, pos) : "";
    }
}
