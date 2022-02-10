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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.PathResolver;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbPack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStreamListener;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;

public class ServerCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCall.class);

    public final ServerStreamListener streamListener = new ServerStreamListenerImpl();
    private final List<HeaderFilter> headerFilters;
    private final GenericUnpack genericUnpack;
    private final FrameworkModel frameworkModel;
    private final ServerStream serverStream;
    private final Executor executor;
    private final String methodName;
    private final String serviceName;
    private final PathResolver pathResolver;
    public boolean autoRequestN = true;
    private Invoker<?> invoker;
    private ServiceDescriptor serviceDescriptor;
    private PbUnpack<?> unpack;
    private PbPack pack;
    private ProviderModel providerModel;
    private List<MethodDescriptor> methodDescriptors;
    private boolean closed;
    private RpcInvocation invocation;
    private Listener listener;
    private boolean headerSent;
    private Compressor compressor;

    public ServerCall(ServerStream serverStream,
                      FrameworkModel frameworkModel,
                      String serviceName,
                      String methodName,
                      Executor executor,
                      List<HeaderFilter> headerFilters,
                      GenericUnpack genericUnpack,
                      PathResolver pathResolver
    ) {
        this.executor = new SerializingExecutor(executor);
        this.frameworkModel = frameworkModel;
        this.methodName = methodName;
        this.serviceName = serviceName;
        this.serverStream = serverStream;
        this.headerFilters = headerFilters;
        this.genericUnpack = genericUnpack;
        this.pathResolver = pathResolver;
    }

    private void sendHeader() {
        if (headerSent) {
            return;
        }
        headerSent = true;
        final DefaultHttp2Headers headers = TripleConstant.createSuccessHttp2Headers();
        if (compressor != null) {
            headers.set(TripleHeaderEnum.GRPC_ENCODING.getHeader(), compressor.getMessageEncoding());
        }
        serverStream.sendHeader(headers);
    }

    public void requestN(int n) {
        serverStream.requestN(n);
    }

    public void setCompression(String compression) {
        if (headerSent) {
            throw new IllegalStateException("Can not set compression after message sent");
        }
        this.compressor = Compressor.getCompressor(frameworkModel, compression);
    }

    public void disableAutoRequestN() {
        autoRequestN = false;
    }

    public void writeMessage(Object message) {
        executor.execute(() -> {
            if (!headerSent) {
                sendHeader();
            }
            final byte[] data;
            try {
                data = pack.pack(message);
            } catch (IOException e) {
                close(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("Serialize response failed")
                    .withCause(e), null);
                return;
            }
            if (data == null) {
                close(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("Missing response"), null);
                return;
            }
            if (compressor != null) {
                int compressedFlag = Identity.MESSAGE_ENCODING.equals(compressor.getMessageEncoding()) ? 0 : 1;
                final byte[] compressed = compressor.compress(data);
                serverStream.writeMessage(compressed, compressedFlag);
            } else {
                serverStream.writeMessage(data, 0);
            }
        });
    }

    public void close(GrpcStatus status, Map<String, Object> trailers) {
        executor.execute(() -> serverStream.close(status, trailers));
    }

    private Invoker<?> getInvoker(Map<String, Object> headers, String serviceName) {
        final String version = headers.containsKey(TripleHeaderEnum.SERVICE_VERSION.getHeader()) ? headers.get(
            TripleHeaderEnum.SERVICE_VERSION.getHeader()).toString() : null;
        final String group = headers.containsKey(TripleHeaderEnum.SERVICE_GROUP.getHeader()) ? headers.get(TripleHeaderEnum.SERVICE_GROUP.getHeader())
            .toString() : null;
        final String key = URL.buildKey(serviceName, group, version);
        Invoker<?> invoker = pathResolver.resolve(key);
        if (invoker == null) {
            invoker = pathResolver.resolve(serviceName);
        }
        return invoker;
    }


    private boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }

    private boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName);
    }

    protected Long parseTimeoutToNanos(String timeoutVal) {
        if (StringUtils.isEmpty(timeoutVal) || StringUtils.isContains(timeoutVal, "null")) {
            return null;
        }
        long value = Long.parseLong(timeoutVal.substring(0, timeoutVal.length() - 1));
        char unit = timeoutVal.charAt(timeoutVal.length() - 1);
        switch (unit) {
            case 'n':
                return value;
            case 'u':
                return TimeUnit.MICROSECONDS.toNanos(value);
            case 'm':
                return TimeUnit.MILLISECONDS.toNanos(value);
            case 'S':
                return TimeUnit.SECONDS.toNanos(value);
            case 'M':
                return TimeUnit.MINUTES.toNanos(value);
            case 'H':
                return TimeUnit.HOURS.toNanos(value);
            default:
                // invalid timeout config
                return null;
        }
    }

    /**
     * Error in create stream, unsupported config or triple protocol error.
     *
     * @param status
     */
    private void responseErr(GrpcStatus status) {
        Http2Headers trailers = new DefaultHttp2Headers()
            .status(OK.codeAsText())
            .set(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO)
            .setInt(TripleHeaderEnum.STATUS_KEY.getHeader(), status.code.code)
            .set(TripleHeaderEnum.MESSAGE_KEY.getHeader(), status.toMessage());
        serverStream.sendHeaderWithEos(trailers);
    }

    interface Listener {

        void onMessage(Object message);

        void onHalfClose();

        void onCancel(String errorInfo);

        void onComplete();
    }

    class ServerStreamListenerImpl implements ServerStreamListener {

        private MethodDescriptor methodDescriptor;
        private Map<String, Object> headers;

        void trySetMethodDescriptor(byte[] data) throws InvalidProtocolBufferException {
            if (methodDescriptor != null) {
                return;
            }
            final TripleWrapper.TripleRequestWrapper request;
            request = TripleWrapper.TripleRequestWrapper.parseFrom(data);

            final String[] paramTypes = request.getArgTypesList().toArray(new String[request.getArgsCount()]);
            // wrapper mode the method can overload so maybe list
            for (MethodDescriptor descriptor : methodDescriptors) {
                // params type is array
                if (Arrays.equals(descriptor.getCompatibleParamSignatures(), paramTypes)) {
                    methodDescriptor = descriptor;
                    break;
                }
            }
            if (methodDescriptor == null) {
                close(GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                    .withDescription("Method :" + methodName + "[" + Arrays.toString(paramTypes) + "] " +
                        "not found of service:" + serviceDescriptor.getServiceName()), null);
            }
        }

        void trySetListener() {
            if (listener != null) {
                return;
            }
            if (methodDescriptor == null) {
                return;
            }
            if (closed) {
                return;
            }
            invocation = buildInvocation(headers);
            if (closed) {
                return;
            }
            headerFilters.forEach(f -> f.invoke(invoker, invocation));
            if (closed) {
                return;
            }

            if (methodDescriptor instanceof StreamMethodDescriptor) {
                if (((StreamMethodDescriptor) methodDescriptor).streamType == StreamMethodDescriptor.StreamType.SERVER) {
                    listener = new ServerStreamServerCallListener(ServerCall.this, invocation, invoker);
                } else {
                    listener = new BiStreamServerCallListener(ServerCall.this, invocation, invoker);
                }
            } else {
                listener = new UnaryServerCallListener(ServerCall.this, invocation, invoker);
            }
            if (methodDescriptor.isNeedWrap()) {
                listener = new WrapRequestServerCallListener(listener, genericUnpack);
            }
        }

        @Override
        public void onHeaders(Map<String, Object> headers) {
            this.headers = headers;
            doOnHeaders(headers);
        }

        private void doOnHeaders(Map<String, Object> headers) {
            invoker = getInvoker(headers, serviceName);
            if (invoker == null) {
                responseErr(GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                    .withDescription("Service not found:" + serviceName));
                return;
            }
            FrameworkServiceRepository repo = frameworkModel.getServiceRepository();
            providerModel = repo.lookupExportedService(invoker.getUrl().getServiceKey());
            if (providerModel == null || providerModel.getServiceModel() == null) {
                responseErr(GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                    .withDescription("Service not found:" + serviceName));
                return;
            }
            serviceDescriptor = providerModel.getServiceModel();

            if (isGeneric(methodName)) {
                // There should be one and only one
                methodDescriptor = ServiceDescriptorInternalCache.genericService().getMethods(methodName).get(0);
            } else if (isEcho(methodName)) {
                // There should be one and only one
                methodDescriptor = ServiceDescriptorInternalCache.echoService().getMethods(methodName).get(0);
            } else {
                methodDescriptors = providerModel.getServiceModel().getMethods(methodName);
                // try upper-case method
                if (CollectionUtils.isEmpty(methodDescriptors)) {
                    final String upperMethod = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
                    methodDescriptors = providerModel.getServiceModel().getMethods(upperMethod);
                }
                if (CollectionUtils.isEmpty(methodDescriptors)) {
                    responseErr(GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                        .withDescription("Method : " + methodName + " not found of service:" + serviceName));
                    return;
                }
                // In most cases there is only one method
                if (methodDescriptors.size() == 1) {
                    methodDescriptor = methodDescriptors.get(0);
                }
            }
            if (methodDescriptor.isNeedWrap()) {
                unpack = PbUnpack.REQ_PB_UNPACK;
            } else {
                if (methodDescriptor instanceof StreamMethodDescriptor) {
                    unpack = new PbUnpack<>(((StreamMethodDescriptor) methodDescriptor).requestType);
                } else {
                    unpack = new PbUnpack<>(methodDescriptor.getParameterClasses()[0]);
                }
            }
            pack = PbPack.INSTANCE;
            trySetListener();
            if (listener == null) {
                // wrap request , need one message
                requestN(1);
            }
        }

        @Override
        public void complete() {
            executor.execute(() -> listener.onComplete());
        }

        @Override
        public void cancel(GrpcStatus status) {
            executor.execute(() -> listener.onCancel(status.description));
        }

        @Override
        public void onMessage(byte[] message) {
            executor.execute(() -> {
                if (closed) {
                    return;
                }
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    trySetMethodDescriptor(message);
                    trySetListener();
                    if (providerModel != null) {
                        ClassLoadUtil.switchContextLoader(providerModel.getServiceInterfaceClass().getClassLoader());
                    }
                    final Object obj = unpack.unpack(message);
                    listener.onMessage(obj);
                } catch (IOException e) {
                    close(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL).withDescription("Server error")
                        .withCause(e), null);
                } finally {
                    ClassLoadUtil.switchContextLoader(tccl);
                }
            });
        }

        /**
         * Build the RpcInvocation with metadata and execute headerFilter
         *
         * @param headers request header
         * @return RpcInvocation
         */
        protected RpcInvocation buildInvocation(Map<String, Object> headers) {
            final URL url = invoker.getUrl();
            RpcInvocation inv = new RpcInvocation(url.getServiceModel(),
                methodName, serviceDescriptor.getServiceName(),
                url.getProtocolServiceKey(), methodDescriptor.getParameterClasses(), new Object[0]);
            inv.setTargetServiceUniqueName(url.getServiceKey());
            inv.setReturnTypes(methodDescriptor.getReturnTypes());

            inv.setObjectAttachments(headers);
            // handle timeout
            String timeout = (String) headers.get(TripleHeaderEnum.TIMEOUT.getHeader());
            try {
                if (!Objects.isNull(timeout)) {
                    final Long timeoutInNanos = parseTimeoutToNanos(timeout.toString());
                    if (!Objects.isNull(timeoutInNanos)) {
                        inv.setAttachment(TIMEOUT_KEY, timeoutInNanos);
                    }
                }
            } catch (Throwable t) {
                LOGGER.warn(String.format("Failed to parse request timeout set from:%s, service=%s method=%s",
                    timeout, serviceDescriptor.getServiceName(), methodName));
            }
            return inv;
        }
    }

}
