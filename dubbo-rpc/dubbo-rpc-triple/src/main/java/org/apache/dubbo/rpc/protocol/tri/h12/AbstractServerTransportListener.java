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
package org.apache.dubbo.rpc.protocol.tri.h12;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.MethodUtils;
import org.apache.dubbo.remoting.http12.ExceptionHandler;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpInputMessage;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.HttpTransportListener;
import org.apache.dubbo.remoting.http12.RequestMetadata;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.remoting.http12.message.MethodMetadata;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.DescriptorUtils;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.RpcInvocationBuildContext;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.CompositeExceptionHandler;
import org.apache.dubbo.rpc.protocol.tri.route.DefaultRequestRouter;
import org.apache.dubbo.rpc.protocol.tri.route.RequestRouter;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_ERROR_USE_THREAD_POOL;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_PARSE;
import static org.apache.dubbo.rpc.protocol.tri.TripleConstant.REMOTE_ADDRESS_KEY;

public abstract class AbstractServerTransportListener<HEADER extends RequestMetadata, MESSAGE extends HttpInputMessage>
        implements HttpTransportListener<HEADER, MESSAGE> {

    private static final ErrorTypeAwareLogger LOGGER =
            LoggerFactory.getErrorTypeAwareLogger(AbstractServerTransportListener.class);

    private final FrameworkModel frameworkModel;
    private final URL url;
    private final HttpChannel httpChannel;
    private final RequestRouter requestRouter;
    private final ExceptionHandler<Throwable, ?> exceptionHandler;
    private final List<HeaderFilter> headerFilters;

    private Executor executor;
    private HEADER httpMetadata;
    private RpcInvocationBuildContext context;
    private HttpMessageListener httpMessageListener;

    protected AbstractServerTransportListener(FrameworkModel frameworkModel, URL url, HttpChannel httpChannel) {
        this.frameworkModel = frameworkModel;
        this.url = url;
        this.httpChannel = httpChannel;
        requestRouter = frameworkModel.getBeanFactory().getOrRegisterBean(DefaultRequestRouter.class);
        exceptionHandler = frameworkModel.getBeanFactory().getOrRegisterBean(CompositeExceptionHandler.class);
        headerFilters = frameworkModel
                .getExtensionLoader(HeaderFilter.class)
                .getActivateExtension(url, CommonConstants.HEADER_FILTER_KEY);
    }

    @Override
    public void onMetadata(HEADER metadata) {
        try {
            executor = initializeExecutor(metadata);
        } catch (Throwable throwable) {
            LOGGER.error(COMMON_ERROR_USE_THREAD_POOL, "", "", "initialize executor fail.", throwable);
            onError(throwable);
            return;
        }
        if (executor == null) {
            LOGGER.error(INTERNAL_ERROR, "", "", "executor must be not null.");
            onError(new NullPointerException("initializeExecutor return null"));
            return;
        }
        executor.execute(() -> {
            try {
                doOnMetadata(metadata);
            } catch (Throwable t) {
                logError(t);
                onError(t);
            }
        });
    }

    protected Executor initializeExecutor(HEADER metadata) {
        // default direct executor
        return Runnable::run;
    }

    protected void doOnMetadata(HEADER metadata) {
        onPrepareMetadata(metadata);
        httpMetadata = metadata;

        context = requestRouter.route(url, metadata, httpChannel);
        if (context == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND.getCode(), "Invoker not found");
        }

        setHttpMessageListener(buildHttpMessageListener());
        onMetadataCompletion(metadata);
    }

    protected abstract HttpMessageListener buildHttpMessageListener();

    @Override
    public void onData(MESSAGE message) {
        executor.execute(() -> {
            try {
                doOnData(message);
            } catch (Throwable t) {
                logError(t);
                onError(message, t);
            } finally {
                onFinally(message);
            }
        });
    }

    protected void doOnData(MESSAGE message) {
        if (httpMessageListener == null) {
            return;
        }
        onPrepareData(message);
        // decode message
        httpMessageListener.onMessage(message.getBody());
        onDataCompletion(message);
    }

    protected void onPrepareMetadata(HEADER header) {
        // default no op
    }

    protected void onMetadataCompletion(HEADER metadata) {
        // default no op
    }

    protected void onPrepareData(MESSAGE message) {
        // default no op
    }

    protected void onDataCompletion(MESSAGE message) {
        // default no op
    }

    protected void logError(Throwable t) {
        t = ExceptionUtils.unwrap(t);
        Supplier<String> msg = () -> {
            StringBuilder sb = new StringBuilder(64);
            sb.append("An error occurred while processing the http request, ");
            sb.append(httpMetadata);
            if (TripleProtocol.VERBOSE_ENABLED) {
                sb.append(", headers=").append(httpMetadata.headers());
            }
            if (context != null) {
                MethodDescriptor md = context.getMethodDescriptor();
                if (md != null) {
                    sb.append(", method=").append(MethodUtils.toShortString(md.getMethod()));
                }
                if (TripleProtocol.VERBOSE_ENABLED) {
                    Invoker<?> invoker = context.getInvoker();
                    if (invoker != null) {
                        URL url = invoker.getUrl();
                        Object service = url.getServiceModel().getProxyObject();
                        sb.append(", service=")
                                .append(service.getClass().getSimpleName())
                                .append('@')
                                .append(Integer.toHexString(System.identityHashCode(service)))
                                .append(", url='")
                                .append(url)
                                .append('\'');
                    }
                }
            }
            return sb.toString();
        };
        switch (exceptionHandler.resolveLogLevel(t)) {
            case TRACE:
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(msg.get(), t);
                }
                return;
            case DEBUG:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(msg.get(), t);
                }
                return;
            case INFO:
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(msg.get(), t);
                }
                return;
            case WARN:
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn(INTERNAL_ERROR, "", "", msg.get(), t);
                }
                return;
            case ERROR:
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error(INTERNAL_ERROR, "", "", msg.get(), t);
                }
                return;
            default:
        }
    }

    protected void onError(Throwable throwable) {
        // default rethrow
        if (throwable instanceof RuntimeException) {
            throw ((RuntimeException) throwable);
        }
        if (throwable instanceof InvocationTargetException) {
            Throwable targetException = ((InvocationTargetException) throwable).getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            }
        }
        throw new HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), throwable);
    }

    protected void onError(MESSAGE message, Throwable throwable) {
        onError(throwable);
    }

    protected void onFinally(MESSAGE message) {
        try {
            message.close();
        } catch (Exception e) {
            onError(e);
        }
    }

    protected RpcInvocation buildRpcInvocation(RpcInvocationBuildContext context) {
        MethodDescriptor methodDescriptor = context.getMethodDescriptor();
        if (methodDescriptor == null) {
            methodDescriptor = DescriptorUtils.findMethodDescriptor(
                    context.getServiceDescriptor(), context.getMethodName(), context.isHasStub());
            context.setMethodDescriptor(methodDescriptor);
            onSettingMethodDescriptor(methodDescriptor);
        }
        MethodMetadata methodMetadata = context.getMethodMetadata();
        if (methodMetadata == null) {
            methodMetadata = MethodMetadata.fromMethodDescriptor(methodDescriptor);
            context.setMethodMetadata(methodMetadata);
        }

        Invoker<?> invoker = context.getInvoker();
        URL url = invoker.getUrl();
        RpcInvocation inv = new RpcInvocation(
                url.getServiceModel(),
                methodDescriptor.getMethodName(),
                context.getServiceDescriptor().getInterfaceName(),
                url.getProtocolServiceKey(),
                methodDescriptor.getParameterClasses(),
                new Object[0]);
        inv.setTargetServiceUniqueName(url.getServiceKey());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        inv.setObjectAttachments(StreamUtils.toAttachments(httpMetadata.headers()));
        inv.put(REMOTE_ADDRESS_KEY, httpChannel.remoteAddress());
        inv.getAttributes().putAll(context.getAttributes());
        String consumerAppName = httpMetadata.headers().getFirst(TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader());
        if (null != consumerAppName) {
            inv.put(TripleHeaderEnum.CONSUMER_APP_NAME_KEY, consumerAppName);
        }
        // customizer RpcInvocation
        headerFilters.forEach(f -> f.invoke(invoker, inv));

        initializeAltSvc(url);

        return onBuildRpcInvocationCompletion(inv);
    }

    /**
     * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Alt-Svc">Alt-Svc</a>
     */
    protected void initializeAltSvc(URL url) {}

    protected RpcInvocation onBuildRpcInvocationCompletion(RpcInvocation invocation) {
        String timeoutString = httpMetadata.headers().getFirst(TripleHeaderEnum.SERVICE_TIMEOUT.getHeader());
        try {
            if (null != timeoutString) {
                Long timeout = Long.parseLong(timeoutString);
                invocation.put(CommonConstants.TIMEOUT_KEY, timeout);
            }
        } catch (Throwable t) {
            LOGGER.warn(
                    PROTOCOL_FAILED_PARSE,
                    "",
                    "",
                    String.format(
                            "Failed to parse request timeout set from:%s, service=%s " + "method=%s",
                            timeoutString, context.getServiceDescriptor().getInterfaceName(), context.getMethodName()));
        }
        return invocation;
    }

    protected final FrameworkModel getFrameworkModel() {
        return frameworkModel;
    }

    protected ExceptionHandler<Throwable, ?> getExceptionHandler() {
        return exceptionHandler;
    }

    protected final HEADER getHttpMetadata() {
        return httpMetadata;
    }

    public final RpcInvocationBuildContext getContext() {
        return context;
    }

    protected final HttpMessageListener getHttpMessageListener() {
        return httpMessageListener;
    }

    protected final void setHttpMessageListener(HttpMessageListener httpMessageListener) {
        this.httpMessageListener = httpMessageListener;
    }

    protected void onSettingMethodDescriptor(MethodDescriptor methodDescriptor) {}
}
