package com.alibaba.dubbo.rpc.protocol.proxy;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ASMJavaBeanDeserializer;
import com.alibaba.fastjson.util.TypeUtils;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/2/1.
 */
public class RestProxyProtocol extends AbstractProxyProtocol {

    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();

    private HttpBinder httpBinder;

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        if (!ProxyService.class.isAssignableFrom(type)) {
            throw new UnsupportedOperationException("Unsupported export proxy service. url: " + url);
        }

        final String addr = url.getIp() + ":" + url.getPort();
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            server = httpBinder.bind(url, new RestProxyHandler());
            serverMap.put(addr, server);
        }

        return new Runnable() {
            public void run() {
                serverMap.get(addr).close();
            }
        };
    }

    @Override
    protected <T> T doRefer(final Class<T> type, final URL url) throws RpcException {

        if (!ProxyService.class.isAssignableFrom(type)) {
            throw new UnsupportedOperationException("Unsupported refer proxy service. url: " + url);
        }

        String scheme = "http://";
        if (url.getPort() == 443 || url.getPort() == 8433) {
            scheme = "https://";
        }

        final String addr = scheme + url.getIp() + ":" + url.getPort() + getContextPath(url);

        int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        final CloseableHttpClient httpclient = HttpClientBuilder.create()
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(connections)
                .setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
                .setMaxConnTotal(connections)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, true))
                .build();
        return (T) Proxy.newProxyInstance(RestProxyProtocol.class.getClassLoader(), new Class<?>[]{type}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("target")) {
                    return target(httpclient, (Class<?>) args[0], url);
                }
                String request = JSON.toJSONString(args[0]);
                Type returnClass = (Type) args[1];

                return execute(httpclient, addr, request, returnClass);
            }
        });
    }

    public <V> V target(final CloseableHttpClient httpClient, final Class<V> iFace, URL url) {
        final String group = url.getParameter("group");
        final String version = url.getParameter("version");
        final String service = iFace.getName();
        String scheme = "http://";
        if (url.getPort() == 443 || url.getPort() == 8433) {
            scheme = "https://";
        }
        final String addr = scheme + url.getIp() + ":" + url.getPort() + getContextPath(url);

        return (V) Proxy.newProxyInstance(RestProxyProtocol.class.getClassLoader(), new Class[]{iFace}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                String methodName = method.getName();
                Class<?>[] parameterTypes = method.getParameterTypes();
                String[] paramsType = new String[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    paramsType[i] = parameterTypes[i].getName();
                }
                String request = JSON.toJSONString(new RestRefConfig(group, version, service, methodName, args, paramsType));
                return execute(httpClient, addr, request, method.getGenericReturnType());
            }
        });
    }

    public Object execute(CloseableHttpClient httpClient, String addr, String request, Type returnClass) throws IOException {
        HttpPost post = new HttpPost(addr);
        post.setEntity(new StringEntity(request, ContentType.APPLICATION_JSON));
        CloseableHttpResponse response = httpClient.execute(post);
        InputStream in = response.getEntity().getContent();
        String result = StreamUtils.copyToString(in, Charset.forName("utf-8"));
        if (response.getStatusLine().getStatusCode() != 200) {
            JSONObject json = JSON.parseObject(result);
            throw new RpcException(json.getString("message"));
        }
        return cast(result, returnClass);
    }


    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    @Override
    public int getDefaultPort() {
        return 8080;
    }

    private class RestProxyHandler implements HttpHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (request.getMethod().equalsIgnoreCase("GET")) {
                response.setStatus(500);
            }
            RpcContext.getContext().setRemoteAddress(request.getRemoteHost(), request.getRemotePort());

            ServletInputStream in = request.getInputStream();
            byte[] bytes = StreamUtils.copyToByteArray(in);
            GenericServiceConfig config = JSON.parseObject(bytes, GenericServiceConfig.class);
            ProxyServiceImpl proxyService = ServiceBean.getSpringContext().getBean(ProxyServiceImpl.class);
            response.setHeader("Content-Type", "text/plain;charset=utf-8");

            Object message;
            try {
                message = proxyService.invoke(config, null);
            } catch (Exception e) {
                e.printStackTrace();
                JSONObject json = new JSONObject();
                json.put("status", 500);
                json.put("message", e.toString());
                message = json;
                response.setStatus(500);
            }
            PrintWriter writer = response.getWriter();
            if (message == null) {
                return ;
            }else if (Integer.class.isInstance(message)) {
                writer.print(Integer.class.cast(message).intValue());
            } else if (Long.class.isInstance(message)) {
                writer.print(Long.class.cast(message).longValue());
            } else if (Float.class.isInstance(message)) {
                writer.print(Float.class.cast(message).floatValue());
            } else if (Double.class.isInstance(message)) {
                writer.print(Double.class.cast(message).doubleValue());
            } else if (BigDecimal.class.isInstance(message)) {
                writer.print(BigDecimal.class.cast(message));
            } else if (String.class.isInstance(message)) {
                writer.print(String.class.cast(message));
            } else {
                response.setHeader("Content-Type", "application/json;charset=utf-8");
                writer.print(JSON.toJSONString(message));
            }
        }
    }

    private String getContextPath(URL url) {
        int pos = url.getPath().lastIndexOf("/");
        return pos > 0 ? url.getPath().substring(0, pos) : "";
    }

    public <T> Object cast(String obj, Type clazz) {

        if (obj == null || StringUtils.isBlank(obj)) {
            return obj;
        }

        JavaType javaType = TypeFactory.defaultInstance().constructType(clazz);
        if (javaType.isCollectionLikeType() || javaType.isArrayType()) {
            return JSON.parseArray(obj, javaType.getContentType().getRawClass());
        } else if (javaType.isMapLikeType()) {
            return JSON.parseObject(obj, javaType.getRawClass());
        } else {
            ParserConfig parserConfig = ParserConfig.getGlobalInstance();

            if (parserConfig.getDeserializer(javaType.getRawClass()) instanceof ASMJavaBeanDeserializer) {
                return JSON.parseObject(obj, javaType.getRawClass());
            } else {
                return TypeUtils.cast(obj, javaType.getRawClass(), ParserConfig.getGlobalInstance());
            }
        }
    }
}
