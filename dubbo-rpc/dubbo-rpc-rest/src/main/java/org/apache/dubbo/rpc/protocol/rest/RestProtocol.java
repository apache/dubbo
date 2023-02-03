/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.rest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.remoting.http.factory.RestClientFactory;
import org.apache.dubbo.remoting.http.servlet.BootstrapListener;
import org.apache.dubbo.remoting.http.servlet.ServletManager;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.model.*;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionConfig;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.annotation.metadata.MetadataResolver;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.response.HttpResponseFacade;
import org.apache.dubbo.rpc.protocol.rest.response.HttpResponseFactory;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;
import org.jboss.resteasy.util.GetRestful;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.*;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;

public class RestProtocol extends AbstractProxyProtocol {

    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_SERVER = "jetty";

    private final RestServerFactory serverFactory = new RestServerFactory();

    private final Map<String, ReferenceCountedClient<? extends RestClient>> clients = new ConcurrentHashMap<>();

    private static final RestClientFactory clientFactory =
        FrameworkModel.defaultModel().getExtensionLoader(RestClientFactory.class).getAdaptiveExtension();

    private static final Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts =
        ApplicationModel.defaultModel().getExtensionLoader(HttpConnectionPreBuildIntercept.class).getSupportedExtensionInstances();

    public RestProtocol() {
        super(WebApplicationException.class, ProcessingException.class);
    }


    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String addr = getAddr(url);
        Class implClass = url.getServiceModel().getProxyObject().getClass();
        RestProtocolServer server = (RestProtocolServer) serverMap.computeIfAbsent(addr, restServer -> {
            RestProtocolServer s = serverFactory.createServer(url.getParameter(SERVER_KEY, DEFAULT_SERVER));
            s.setAddress(url.getAddress());
            s.start(url);
            return s;
        });

        String contextPath = getContextPath(url);
        if ("servlet".equalsIgnoreCase(url.getParameter(SERVER_KEY, DEFAULT_SERVER))) {
            ServletContext servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
            if (servletContext == null) {
                throw new RpcException("No servlet context found. Since you are using server='servlet', " +
                    "make sure that you've configured " + BootstrapListener.class.getName() + " in web.xml");
            }
            String webappPath = servletContext.getContextPath();
            if (StringUtils.isNotEmpty(webappPath)) {
                webappPath = webappPath.substring(1);
                if (!contextPath.startsWith(webappPath)) {
                    throw new RpcException("Since you are using server='servlet', " +
                        "make sure that the 'contextpath' property starts with the path of external webapp");
                }
                contextPath = contextPath.substring(webappPath.length());
                if (contextPath.startsWith("/")) {
                    contextPath = contextPath.substring(1);
                }
            }
        }

        final Class resourceDef = GetRestful.getRootResourceClass(implClass) != null ? implClass : type;

        server.deploy(resourceDef, impl, contextPath);

        final RestProtocolServer s = server;
        return () -> {
            // TODO due to dubbo's current architecture,
            // it will be called from registry protocol in the shutdown process and won't appear in logs
            s.undeploy(resourceDef);
        };
    }


    @Override
    protected <T> Invoker<T> protocolBindingRefer(final Class<T> type, final URL url) throws RpcException {

        ReferenceCountedClient<? extends RestClient> refClient =
            clients.computeIfAbsent(url.getAddress(), key -> createReferenceCountedClient(url));

        refClient.retain();

        // resolve metadata
        Map<Method, RestMethodMetadata> metadataMap = MetadataResolver.resolveConsumerServiceMetadata(type, url);


        Invoker<T> invoker = new AbstractInvoker<T>(type, url, new String[]{INTERFACE_KEY, GROUP_KEY, TOKEN_KEY}) {
            @Override
            protected Result doInvoke(Invocation invocation) {
                try {

                    RpcInvocation rpcInvocation = (RpcInvocation) invocation;

                    ConsumerMethodModel consumerMethodModel = (ConsumerMethodModel) rpcInvocation.get(Constants.METHOD_MODEL);

                    RestMethodMetadata restMethodMetadata = metadataMap.get(consumerMethodModel.getMethod());

                    RequestTemplate requestTemplate = new RequestTemplate(restMethodMetadata.getRequest().getMethod(), url.getAddress());

                    // TODO  dynamic load config
                    HttpConnectionConfig connectionConfig = new HttpConnectionConfig();

                    HttpConnectionCreateContext httpConnectionCreateContext = createBuildContext(requestTemplate,
                        connectionConfig,
                        restMethodMetadata, Arrays.asList(invocation.getArguments()), url);

                    for (HttpConnectionPreBuildIntercept intercept : httpConnectionPreBuildIntercepts) {

                        intercept.intercept(httpConnectionCreateContext);
                    }

                    Object response = refClient.getClient().send(requestTemplate);

                    HttpResponseFacade responseFacade = HttpResponseFactory.
                        createFacade(url.getParameter(org.apache.dubbo.remoting.Constants.CLIENT_KEY, "okhttp"), response);

                    // TODO response code
                    int responseCode = responseFacade.getResponseCode();


                    Object value = HttpMessageCodecManager.httpMessageDecode(responseFacade.getBody(),
                        restMethodMetadata.getReflectMethod().getReturnType(),
                        MediaTypeUtil.convertMediaType(responseFacade.getContentType()));

                    CompletableFuture<Object> future = wrapWithFuture(value, invocation);

                    CompletableFuture<AppResponse> appResponseFuture = future.handle((obj, t) -> {
                        AppResponse result = new AppResponse(invocation);
                        if (t != null) {
                            if (t instanceof CompletionException) {
                                result.setException(t.getCause());
                            } else {
                                result.setException(t);
                            }
                        } else {
                            result.setValue(obj);
                        }
                        return result;
                    });
                    return new AsyncRpcResult(appResponseFuture, invocation);

                } catch (RpcException e) {
                    if (e.getCode() == RpcException.UNKNOWN_EXCEPTION) {
                        e.setCode(getErrorCode(e.getCause()));
                    }
                    throw e;
                } catch (InvocationTargetException e) {
                    if (RpcContext.getServiceContext().isAsyncStarted() && !RpcContext.getServiceContext().stopAsync()) {
                        logger.error(PROXY_ERROR_ASYNC_RESPONSE, "", "", "Provider async started, but got an exception from the original method, cannot write the exception back to consumer because an async result may have returned the new thread.", e);
                    }
                    return AsyncRpcResult.newDefaultAsyncResult(null, e.getTargetException(), invocation);
                } catch (Throwable e) {
                    throw new RpcException("Failed to invoke remote proxy method " + invocation.getMethodName() + " to " + getUrl() + ", cause: " + e.getMessage(), e);
                }
            }

            @Override
            public void destroy() {
                super.destroy();
                invokers.remove(this);
                destroyInternal(url);
            }
        };
        invokers.add(invoker);
        return invoker;
    }


    private ReferenceCountedClient<? extends RestClient> createReferenceCountedClient(URL url) throws RpcException {

        // url -> RestClient
        RestClient restClient = clientFactory.createRestClient(url);

        return new ReferenceCountedClient<>(restClient);
    }

    @Override
    protected int getErrorCode(Throwable e) {
        // TODO
        return super.getErrorCode(e);
    }

    @Override
    public void destroy() {
        if (logger.isInfoEnabled()) {
            logger.info("Destroying protocol [" + this.getClass().getSimpleName() + "] ...");
        }
        super.destroy();

        for (Map.Entry<String, ProtocolServer> entry : serverMap.entrySet()) {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Closing the rest server at " + entry.getKey());
                }
                entry.getValue().close();
            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_SERVER, "", "", "Error closing rest server", t);
            }
        }
        serverMap.clear();

        if (logger.isInfoEnabled()) {
            logger.info("Closing rest clients");
        }
        for (ReferenceCountedClient client : clients.values()) {
            try {
                // destroy directly regardless of the current reference count.
                client.destroy();
            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "Error closing rest client", t);
            }
        }
        clients.clear();
    }

    /**
     * getPath() will return: [contextpath + "/" +] path
     * 1. contextpath is empty if user does not set through ProtocolConfig or ProviderConfig
     * 2. path will never be empty, its default value is the interface name.
     *
     * @return return path only if user has explicitly gave then a value.
     */
    protected String getContextPath(URL url) {
        String contextPath = url.getPath();
        if (contextPath != null) {
            if (contextPath.equalsIgnoreCase(url.getParameter(INTERFACE_KEY))) {
                return "";
            }
            if (contextPath.endsWith(url.getParameter(INTERFACE_KEY))) {
                contextPath = contextPath.substring(0, contextPath.lastIndexOf(url.getParameter(INTERFACE_KEY)));
            }
            return contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
        } else {
            return "";
        }
    }

    @Override
    protected void destroyInternal(URL url) {
        try {
            ReferenceCountedClient referenceCountedClient = clients.get(url.getAddress());
            if (referenceCountedClient != null && referenceCountedClient.release()) {
                clients.remove(url.getAddress());
            }
        } catch (Exception e) {
            logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "Failed to close unused resources in rest protocol. interfaceName [" + url.getServiceInterface() + "]", e);
        }
    }

    private CompletableFuture<Object> wrapWithFuture(Object value, Invocation invocation) {
        if (value instanceof CompletableFuture) {
            invocation.put(PROVIDER_ASYNC_KEY, Boolean.TRUE);
            return (CompletableFuture<Object>) value;
        } else if (RpcContext.getServerAttachment().isAsyncStarted()) {
            invocation.put(PROVIDER_ASYNC_KEY, Boolean.TRUE);
            return ((AsyncContextImpl) (RpcContext.getServerAttachment().getAsyncContext())).getInternalFuture();
        }
        return CompletableFuture.completedFuture(value);
    }

    private static HttpConnectionCreateContext createBuildContext(RequestTemplate requestTemplate,
                                                                  HttpConnectionConfig connectionConfig,
                                                                  RestMethodMetadata restMethodMetadata, List<Object> rags, URL url) {
        HttpConnectionCreateContext httpConnectionCreateContext = new HttpConnectionCreateContext();
        httpConnectionCreateContext.setConnectionConfig(connectionConfig);
        httpConnectionCreateContext.setRequestTemplate(requestTemplate);
        httpConnectionCreateContext.setRestMethodMetadata(restMethodMetadata);
        httpConnectionCreateContext.setMethodRealArgs(rags);
        httpConnectionCreateContext.setUrl(url);
        return httpConnectionCreateContext;
    }


}
