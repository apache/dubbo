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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.ParameterTypesComparator;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.remoting.http.RestResult;
import org.apache.dubbo.remoting.http.factory.RestClientFactory;
import org.apache.dubbo.remoting.http.servlet.BootstrapListener;
import org.apache.dubbo.remoting.http.servlet.ServletManager;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.AbstractInvoker;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionConfig;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.metadata.MetadataResolver;
import org.apache.dubbo.rpc.protocol.rest.exception.HttpClientException;
import org.apache.dubbo.rpc.protocol.rest.exception.RemoteServerInternalException;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import org.jboss.resteasy.util.GetRestful;

import javax.servlet.ServletContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_SERVER;
import static org.apache.dubbo.remoting.Constants.SERVER_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.protocol.rest.constans.RestConstant.PATH_SEPARATOR;

public class RestProtocol extends AbstractProxyProtocol {

    private static final int DEFAULT_PORT = 80;
    private static final String DEFAULT_SERVER = Constants.JETTY;

    private final RestServerFactory serverFactory = new RestServerFactory();

    private final ConcurrentMap<String, ReferenceCountedClient<? extends RestClient>> clients = new ConcurrentHashMap<>();

    private final RestClientFactory clientFactory;

    private final Set<HttpConnectionPreBuildIntercept> httpConnectionPreBuildIntercepts;

    public RestProtocol(FrameworkModel frameworkModel) {
        super(WebApplicationException.class, ProcessingException.class);
        this.clientFactory = frameworkModel.getExtensionLoader(RestClientFactory.class).getAdaptiveExtension();
        this.httpConnectionPreBuildIntercepts = frameworkModel.getExtensionLoader(HttpConnectionPreBuildIntercept.class).getSupportedExtensionInstances();
    }


    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        String addr = getAddr(url);
        Class<?> implClass = url.getServiceModel().getProxyObject().getClass();
        RestProtocolServer server = (RestProtocolServer) ConcurrentHashMapUtils.computeIfAbsent(serverMap, addr, restServer -> {
            RestProtocolServer s = serverFactory.createServer(url.getParameter(SERVER_KEY, DEFAULT_SERVER));
            s.setAddress(url.getAddress());
            s.start(url);
            return s;
        });

        String contextPath = getContextPath(url);
        if (Constants.SERVLET.equalsIgnoreCase(url.getParameter(SERVER_KEY, DEFAULT_SERVER))) {
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
                if (contextPath.startsWith(PATH_SEPARATOR)) {
                    contextPath = contextPath.substring(1);
                }
            }
        }

        final Class<?> resourceDef = GetRestful.getRootResourceClass(implClass) != null ? implClass : type;

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

        ReferenceCountedClient<? extends RestClient> refClient = clients.get(url.getAddress());
        if (refClient == null || refClient.isDestroyed()) {
            synchronized (clients) {
                refClient = clients.get(url.getAddress());
                if (refClient == null || refClient.isDestroyed()) {
                    refClient = ConcurrentHashMapUtils.computeIfAbsent(clients, url.getAddress(), _key -> createReferenceCountedClient(url));
                }
            }
        }
        refClient.retain();

        final ReferenceCountedClient<? extends RestClient> glueRefClient = refClient;

        // resolve metadata
        Map<String, Map<ParameterTypesComparator, RestMethodMetadata>> metadataMap = MetadataResolver.resolveConsumerServiceMetadata(type, url);

        Invoker<T> invoker = new AbstractInvoker<T>(type, url, new String[]{INTERFACE_KEY, GROUP_KEY, TOKEN_KEY}) {
            @Override
            protected Result doInvoke(Invocation invocation) {
                try {
                    RestMethodMetadata restMethodMetadata = metadataMap.get(invocation.getMethodName()).get(ParameterTypesComparator.getInstance(invocation.getParameterTypes()));

                    RequestTemplate requestTemplate = new RequestTemplate(invocation, restMethodMetadata.getRequest().getMethod(), url.getAddress(), getContextPath(url));

                    HttpConnectionCreateContext httpConnectionCreateContext = new HttpConnectionCreateContext();
                    // TODO  dynamic load config
                    httpConnectionCreateContext.setConnectionConfig(new HttpConnectionConfig());
                    httpConnectionCreateContext.setRequestTemplate(requestTemplate);
                    httpConnectionCreateContext.setRestMethodMetadata(restMethodMetadata);
                    httpConnectionCreateContext.setInvocation(invocation);
                    httpConnectionCreateContext.setUrl(url);

                    for (HttpConnectionPreBuildIntercept intercept : httpConnectionPreBuildIntercepts) {
                        intercept.intercept(httpConnectionCreateContext);
                    }

                    CompletableFuture<RestResult> future = glueRefClient.getClient().send(requestTemplate);
                    CompletableFuture<AppResponse> responseFuture = new CompletableFuture<>();
                    AsyncRpcResult asyncRpcResult = new AsyncRpcResult(responseFuture, invocation);
                    future.whenComplete((r, t) -> {
                        if (t != null) {
                            responseFuture.completeExceptionally(t);
                        } else {
                            AppResponse appResponse = new AppResponse();
                            try {
                                int responseCode = r.getResponseCode();
                                MediaType mediaType = MediaType.TEXT_PLAIN;

                                if (400 < responseCode && responseCode < 500) {
                                    throw new HttpClientException(r.getMessage());
                                } else if (responseCode >= 500) {
                                    throw new RemoteServerInternalException(r.getMessage());
                                } else if (responseCode < 400) {
                                    mediaType = MediaTypeUtil.convertMediaType(r.getContentType());
                                }


                                Object value = HttpMessageCodecManager.httpMessageDecode(r.getBody(),
                                    restMethodMetadata.getReflectMethod().getReturnType(), mediaType);
                                appResponse.setValue(value);
                                Map<String, String> headers = r.headers()
                                    .entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
                                appResponse.setAttachments(headers);
                                responseFuture.complete(appResponse);
                            } catch (Exception e) {
                                responseFuture.completeExceptionally(e);
                            }
                        }
                    });
                    return asyncRpcResult;
                } catch (RpcException e) {
                    if (e.getCode() == RpcException.UNKNOWN_EXCEPTION) {
                        e.setCode(getErrorCode(e.getCause()));
                    }
                    throw e;
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
        for (ReferenceCountedClient<?> client : clients.values()) {
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
            return contextPath.endsWith(PATH_SEPARATOR) ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
        } else {
            return "";
        }
    }

    @Override
    protected void destroyInternal(URL url) {
        try {
            ReferenceCountedClient<?> referenceCountedClient = clients.get(url.getAddress());
            if (referenceCountedClient != null && referenceCountedClient.release()) {
                clients.remove(url.getAddress());
            }
        } catch (Exception e) {
            logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "Failed to close unused resources in rest protocol. interfaceName [" + url.getServiceInterface() + "]", e);
        }
    }
}
