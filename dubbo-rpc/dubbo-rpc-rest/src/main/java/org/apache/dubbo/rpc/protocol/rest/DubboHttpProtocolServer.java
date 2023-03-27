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
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.remoting.http.HttpBinder;
import org.apache.dubbo.remoting.http.HttpHandler;
import org.apache.dubbo.remoting.http.HttpServer;
import org.apache.dubbo.remoting.http.servlet.BootstrapListener;
import org.apache.dubbo.remoting.http.servlet.ServletManager;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.exception.ParamParseException;
import org.apache.dubbo.rpc.protocol.rest.exception.PathNoFoundException;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacadeFactory;
import org.apache.dubbo.rpc.protocol.rest.util.HttpHeaderUtil;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;
import org.apache.dubbo.rpc.protocol.rest.util.Pair;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

public class DubboHttpProtocolServer extends BaseRestProtocolServer {

    private final HttpServletDispatcher dispatcher = new HttpServletDispatcher();
    private final ResteasyDeployment deployment = new ResteasyDeployment();
    private HttpBinder httpBinder;
    private HttpServer httpServer;
//    private boolean isExternalServer;

    public DubboHttpProtocolServer(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    @Override
    protected void doStart(URL url) {
        // TODO jetty will by default enable keepAlive so the xml config has no effect now
        httpServer = httpBinder.bind(url, new RestHandler());

        ServletContext servletContext = ServletManager.getInstance().getServletContext(url.getPort());
        if (servletContext == null) {
            servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
        }
        if (servletContext == null) {
            throw new RpcException("No servlet context found. If you are using server='servlet', " +
                "make sure that you've configured " + BootstrapListener.class.getName() + " in web.xml");
        }

        servletContext.setAttribute(ResteasyDeployment.class.getName(), deployment);

        try {
            dispatcher.init(new SimpleServletConfig(servletContext));
        } catch (ServletException e) {
            throw new RpcException(e);
        }
    }

    @Override
    public void close() {
        httpServer.close();
    }

    @Override
    protected ResteasyDeployment getDeployment() {
        return deployment;
    }

    private class RestHandler implements HttpHandler<HttpServletRequest, HttpServletResponse> {

        @Override
        public void handle(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws IOException {

            RequestFacade request = RequestFacadeFactory.createRequestFacade(servletRequest);
            RpcContext.getServiceContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
//            dispatcher.service(request, servletResponse);


            HttpHeaderUtil.addProviderAttachments(servletResponse);

            Pair<RpcInvocation, Invoker> build = null;
            try {
                build = RPCInvocationBuilder.build(request, servletRequest, servletResponse);
            } catch (PathNoFoundException e) {
                servletResponse.setStatus(404);
            } catch (ParamParseException e) {
                servletResponse.setStatus(400);
            } catch (Throwable throwable) {
                servletResponse.setStatus(500);
            }

            // build RpcInvocation failed ,directly return
            if (build == null) {
                return;
            }


            Invoker invoker = build.getSecond();

            Result invoke = invoker.invoke(build.getFirst());

            if (invoke.hasException()) {

                if (ExceptionMapper.hasExceptionMapper(invoke.getException())) {
                    writeResult(servletResponse, request, invoker, ExceptionMapper.exceptionToResult(invoke.getException()));
                } else {
                    servletResponse.setStatus(500);
                }

            } else {
                Object value = invoke.getValue();
                writeResult(servletResponse, request, invoker, value);
            }


        }


        private void writeResult(HttpServletResponse httpServletResponse, RequestFacade request, Invoker invoker, Object value) {
            try {
                String accept = request.getHeader(RestConstant.ACCEPT);
                MediaType mediaType = MediaTypeUtil.convertMediaType(accept);

                Pair<Boolean, MediaType> booleanMediaTypePair = HttpMessageCodecManager.httpMessageEncode(httpServletResponse.getOutputStream(), value, invoker.getUrl(), mediaType);

                Boolean encoded = booleanMediaTypePair.getFirst();

                if (encoded) {
                    httpServletResponse.addHeader(RestConstant.CONTENT_TYPE, booleanMediaTypePair.getSecond().value);
                }


                httpServletResponse.setStatus(200);
            } catch (Throwable e) {
                httpServletResponse.setStatus(500);
            }
        }
    }

    private static class SimpleServletConfig implements ServletConfig {

        private final ServletContext servletContext;

        public SimpleServletConfig(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        @Override
        public String getServletName() {
            return "DispatcherServlet";
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        @Override
        public String getInitParameter(String s) {
            return null;
        }

        @Override
        public Enumeration getInitParameterNames() {
            return new Enumeration() {
                @Override
                public boolean hasMoreElements() {
                    return false;
                }

                @Override
                public Object nextElement() {
                    return null;
                }
            };
        }
    }
}
