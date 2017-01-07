package com.alibaba.dubbo.rpc.protocol.springmvc;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.remoting.http.servlet.BootstrapListener;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.springmvc.util.SpringUtil;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * @author wuyu DATA:2016-6-18
 */

public class SpringMvcHttpServer {

    private DispatcherServlet dispatcher = new DispatcherServlet();
    private HttpBinder httpBinder;
    private HttpServer httpServer;

    public SpringMvcHttpServer(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    public void stop() {
        dispatcher.destroy();
        httpServer.close();
    }

    protected void doStart(URL url) {
        httpServer = httpBinder.bind(url, new SpringMvcHandler());

        ServletContext servletContext = ServletManager.getInstance().getServletContext(url.getPort());
        if (servletContext == null) {
            servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
        }
        if (servletContext == null) {
            throw new RpcException("No servlet context found. If you are using server='servlet', "
                    + "make sure that you've configured " + BootstrapListener.class.getName() + " in web.xml");
        }


        try {
            dispatcher.setContextConfigLocation("classpath:META-INF/spring/dubbo-springmvc.xml");
            dispatcher.init(new SimpleServletConfig(servletContext));

        } catch (Exception e) {
            throw new RpcException(e);
        }
    }


    private class SpringMvcHandler implements HttpHandler {
        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
            dispatcher.service(request, response);
        }
    }

    public void start(URL url) {
        doStart(url);
    }

    @SuppressWarnings("unchecked")
    public void deploy(Class resourceDef) {

        try {
            // 反射SpringExtensionFactory 拿到所有的ApplicatonContext 通过class类型获取bean
            Set<Object> beans = SpringUtil.getBeans(resourceDef);
            for (Object bean : beans) {
                detectHandlerMethods(bean);
            }

        } catch (Exception e) {
            throw new RpcException(e);
        }

    }

    public void undeploy(Class resourceDef) {
        Set<Object> beans = SpringUtil.getBeans(resourceDef);
        for (Object bean : beans) {
            try {
                unRegisterHandler(bean);
            } catch (Exception e) {

            }
        }
    }

    //注册handler
    private void detectHandlerMethods(Object handler) throws Exception {
        RequestMappingHandlerMapping requestMapping = dispatcher.getWebApplicationContext().getBean(RequestMappingHandlerMapping.class);
        Method detectHandlerMethods = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "detectHandlerMethods", Object.class);
        detectHandlerMethods.setAccessible(true);
        detectHandlerMethods.invoke(requestMapping, handler);
    }


    private void unRegisterHandler(Object handler) throws Exception {
        Set<Method> methods = selectMethods(handler);
        for (Method method : methods) {
            RequestMappingInfo requestMappingInfo = getMappingForMethod(method, handler.getClass());
            if(requestMappingInfo!=null){
                removeHandlerMethod(requestMappingInfo);
            }
        }
    }


    private void removeHandlerMethod(RequestMappingInfo requestMappingInfo) throws Exception {
        RequestMappingHandlerMapping requestMapping = dispatcher.getWebApplicationContext().getBean(RequestMappingHandlerMapping.class);
        Field handlerMethodsFiled = ReflectionUtils.findField(RequestMappingHandlerMapping.class, "handlerMethods");
        if (handlerMethodsFiled == null) {
            Method unregisterMapping = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "unregisterMapping", Object.class);
            unregisterMapping.setAccessible(true);
            unregisterMapping.invoke(requestMappingInfo);
        }else{
            handlerMethodsFiled.setAccessible(true);
            Map handlerMethods = (Map) handlerMethodsFiled.get(requestMapping);
            handlerMethods.remove(requestMapping);
        }
    }


    private RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) throws Exception {
        RequestMappingHandlerMapping requestMapping = dispatcher.getWebApplicationContext().getBean(RequestMappingHandlerMapping.class);
        Method getMappingForMethod = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class,
                "registerHandlerMethod", Method.class, Object.class);
        method.setAccessible(true);
        return (RequestMappingInfo) getMappingForMethod.invoke(requestMapping, method, handlerType);
    }

    private Set<Method> selectMethods(Object handler) {
        Class<?> handlerType = handler.getClass();
        final Class<?> userType = ClassUtils.getUserClass(handlerType);
        Set<Method> methods = HandlerMethodSelector.selectMethods(userType, new ReflectionUtils.MethodFilter() {

            public boolean matches(Method method) {
                return true;
            }
        });
        return methods;
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
