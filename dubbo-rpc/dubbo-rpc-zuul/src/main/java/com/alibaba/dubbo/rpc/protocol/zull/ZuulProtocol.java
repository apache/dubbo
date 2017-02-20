package com.alibaba.dubbo.rpc.protocol.zull;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.filters.FilterRegistry;
import com.netflix.zuul.http.ZuulServlet;
import com.netflix.zuul.monitoring.MonitoringHelper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/2/12.
 */
public class ZuulProtocol extends AbstractProxyProtocol {

    private HttpBinder httpBinder;

    private final Map<Integer, ZuulServlet> zuulServletMap = new ConcurrentHashMap<>();


    @Override
    public int getDefaultPort() {
        return 8080;
    }

    @Override
    protected <T> Runnable doExport(T impl, final Class<T> type, URL url) throws RpcException {
        ZuulServlet zuulServlet = zuulServletMap.get(url.getPort());
        if (zuulServlet == null) {
            zuulServlet = new ZuulServlet();
            try {
                zuulServlet.init(new SimpleServletConfig(ServletManager.getInstance().getServletContext(url.getPort())));
                MonitoringHelper.initMocks();
                httpBinder.bind(url, new ZuulHttpHandler());
            } catch (ServletException e) {
                throw new RpcException(e);
            }
            zuulServletMap.put(url.getPort(), zuulServlet);
        }

        final ZuulFilter zuulFilter = ServiceBean.getSpringContext().getBean(ZuulFilter.class);
        FilterRegistry.instance().put(zuulFilter.disablePropertyName(), zuulFilter);
        return new Runnable() {
            @Override
            public void run() {
                FilterRegistry.instance().remove(zuulFilter.disablePropertyName());
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        return null;
    }

    private class ZuulHttpHandler implements HttpHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            RpcContext.getContext().setRemoteAddress(request.getRemoteHost(), request.getRemotePort());
            RequestContext.getCurrentContext().setRequest(request);
            RequestContext.getCurrentContext().setResponse(response);
            zuulServletMap.get(request.getServerPort()).service(request, response);
        }
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
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

}
