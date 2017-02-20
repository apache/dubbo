package com.alibaba.dubbo.rpc.protocol.zull;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.util.HTTPRequestUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Created by wuyu on 2017/2/13.
 */
public class ZuulRouteFilter extends ZuulFilter {

    private HttpClient httpClient;

    private int timeout = 3000;

    private int connections = 200;

    public static final String CONTENT_ENCODING = "Content-Encoding";


    public ZuulRouteFilter(int timeout, int connections) {
        this.timeout = timeout;
        this.connections = connections;
        initHttpClient();
    }

    public ZuulRouteFilter(HttpClient httpClient, int timeout, int connections) {
        this.httpClient = httpClient;
        this.timeout = timeout;
        this.connections = connections;
        initHttpClient();
    }

    public ZuulRouteFilter() {
        initHttpClient();
    }

    @Override
    public String filterType() {
        return "route";
    }

    @Override
    public int filterOrder() {
        return 100;
    }

    @Override
    public boolean shouldFilter() {
        return RequestContext.getCurrentContext().getRouteHost() != null && RequestContext.getCurrentContext().sendZuulResponse();
    }

    @Override
    public Object run() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        BasicHeader[] headers = buildZuulRequestHeaders(request);
        try {
            InputStream requestEntity = request.getInputStream();
            String uri = request.getRequestURI();
            HttpResponse response = forward(httpClient, getVerb(request.getMethod()), uri, request, headers, requestEntity);
            setResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private HttpResponse forward(HttpClient httpclient, String verb, String uri, HttpServletRequest request, Header[] headers, InputStream requestEntity) throws IOException {

        HttpHost httpHost = getHttpHost();
        HttpRequest httpRequest;

        switch (verb) {
            case "POST":
                HttpPost httpPost = new HttpPost(uri + getQueryString());
                InputStreamEntity entity = new InputStreamEntity(requestEntity);
                httpPost.setEntity(entity);
                httpRequest = httpPost;
                break;
            case "PUT":
                HttpPut httpPut = new HttpPut(uri + getQueryString());
                entity = new InputStreamEntity(requestEntity, request.getContentLength());
                httpPut.setEntity(entity);
                httpRequest = httpPut;
                break;
            default:
                httpRequest = new BasicHttpRequest(verb, uri + getQueryString());
        }

        httpRequest.setHeaders(headers);
        return forwardRequest(httpclient, httpHost, httpRequest);
    }

    HttpResponse forwardRequest(HttpClient httpclient, HttpHost httpHost, HttpRequest httpRequest) throws IOException {
        return httpclient.execute(httpHost, httpRequest);
    }


    BasicHeader[] buildZuulRequestHeaders(HttpServletRequest request) {

        Enumeration headerNames = request.getHeaderNames();
        ArrayList<BasicHeader> headers = new ArrayList<>(request.getParameterMap().size());

        while (headerNames.hasMoreElements()) {
            String name = ((String) headerNames.nextElement()).toLowerCase();
            String value = request.getHeader(name);
            if (isValidHeader(name)) headers.add(new BasicHeader(name, value));
        }


        if (RequestContext.getCurrentContext().getResponseGZipped()) {
            headers.add(new BasicHeader("accept-encoding", "deflate, gzip"));
        }

        BasicHeader[] hArray = new BasicHeader[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            hArray[i] = headers.get(i);
        }
        return hArray;
    }


    void setResponse(HttpResponse response) {
        RequestContext context = RequestContext.getCurrentContext();

        RequestContext.getCurrentContext().set("hostZuulResponse", response);
        RequestContext.getCurrentContext().setResponseStatusCode(response.getStatusLine().getStatusCode());

        boolean isOriginResponseGzipped = false;

        for (Header h : response.getHeaders(CONTENT_ENCODING)) {
            if (HTTPRequestUtils.getInstance().isGzipped(h.getValue())) {
                isOriginResponseGzipped = true;
                break;
            }
        }
        context.setResponseGZipped(isOriginResponseGzipped);

        for (Header header : response.getAllHeaders()) {
            RequestContext ctx = RequestContext.getCurrentContext();
            ctx.addOriginResponseHeader(header.getName(), header.getValue());
            if (header.getName().equalsIgnoreCase("content-length"))
                ctx.setOriginContentLength(header.getValue());

            if (isValidHeader(header)) {
                ctx.addZuulResponseHeader(header.getName(), header.getValue());
            }
        }

    }

    private HttpHost getHttpHost() {
        HttpHost httpHost;
        URL host = RequestContext.getCurrentContext().getRouteHost();

        httpHost = new HttpHost(host.getHost(), host.getPort(), host.getProtocol());

        return httpHost;
    }


    String getVerb(String sMethod) {
        if (sMethod == null) {
            return "GET";
        }

        sMethod = sMethod.toLowerCase();

        if (sMethod.equalsIgnoreCase("post")) {
            return "POST";
        }
        if (sMethod.equalsIgnoreCase("put")) {
            return "PUT";
        }
        if (sMethod.equalsIgnoreCase("delete")) {
            return "DELETE";
        }
        if (sMethod.equalsIgnoreCase("options")) {
            return "OPTIONS";
        }
        if (sMethod.equalsIgnoreCase("head")) {
            return "HEAD";
        }
        return "GET";
    }

    private String getQueryString() {
        String encoding = "UTF-8";
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        String currentQueryString = request.getQueryString();
        if (currentQueryString == null || currentQueryString.equals("")) {
            return "";
        }
        return currentQueryString;
    }

    boolean isValidHeader(String name) {
        if (name.toLowerCase().contains("content-length")) {
            return false;
        }

        if (name.toLowerCase().equals("host")) {
            return false;
        }

        if (!RequestContext.getCurrentContext().getResponseGZipped()) {
            if (name.toLowerCase().contains("accept-encoding")) {
                return false;
            }
        }
        return true;
    }

    private void initHttpClient() {

        SSLContext sslContext = SSLContexts.createSystemDefault();

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", new SSLConnectionSocketFactory(sslContext))
                .build();

        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setSocketTimeout(-1)
                .build();
        httpClient = HttpClientBuilder.create()
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnPerRoute(connections)
                .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                .setMaxConnTotal(connections)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(0, true))
                .build();
    }

    boolean isValidHeader(Header header) {
        switch (header.getName().toLowerCase()) {
            case "connection":
            case "content-length":
            case "content-encoding":
            case "server":
            case "transfer-encoding":
                return false;
            default:
                return true;
        }
    }

}
