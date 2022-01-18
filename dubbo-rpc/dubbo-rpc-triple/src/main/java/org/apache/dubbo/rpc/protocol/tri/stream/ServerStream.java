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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.protocol.tri.DefaultMetadata;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.Metadata;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Any;
import com.google.rpc.DebugInfo;
import com.google.rpc.Status;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.apache.dubbo.common.constants.CommonConstants.HEADER_FILTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.rpc.protocol.tri.GrpcStatus.getStatus;

public class ServerStream extends AbstractStream implements Stream{
    private final ProviderModel providerModel;
    private final List<HeaderFilter> headerFilters;
    private ServiceDescriptor serviceDescriptor;
    private List<MethodDescriptor> methodDescriptors;
    private Invoker<?> invoker;

    protected ServerStream(URL url, Executor executor, ProviderModel providerModel) {
        super(url, executor);
        this.providerModel = providerModel;
        this.headerFilters = url.getOrDefaultApplicationModel().getExtensionLoader(HeaderFilter.class).getActivateExtension(url, HEADER_FILTER_KEY);
    }

    /**
     * Build the RpcInvocation with metadata and execute headerFilter
     *
     * @param metadata request header
     * @return RpcInvocation
     */
    protected RpcInvocation buildInvocation(Metadata metadata) {
        RpcInvocation inv = new RpcInvocation(url().getServiceModel(),
            getMethodName(), getServiceDescriptor().getServiceName(),
            url().getProtocolServiceKey(), getMethodDescriptor().getRealParameterClasses(), new Object[0]);
        inv.setTargetServiceUniqueName(url().getServiceKey());
        inv.setReturnTypes(getMethodDescriptor().getReturnTypes());

        final Map<String, Object> attachments = parseMetadataToAttachmentMap(metadata);
        inv.setObjectAttachments(attachments);
        // handle timeout
        CharSequence timeout = metadata.get(TripleHeaderEnum.TIMEOUT.getHeader());
        try {
            if (!Objects.isNull(timeout)) {
                final Long timeoutInNanos = parseTimeoutToNanos(timeout.toString());
                if (!Objects.isNull(timeoutInNanos)) {
                    inv.setAttachment(TIMEOUT_KEY, timeoutInNanos);
                }
            }
        } catch (Throwable t) {
            LOGGER.warn(String.format("Failed to parse request timeout set from:%s, service=%s method=%s", timeout, getServiceDescriptor().getServiceName(),
                getMethodName()));
        }
        invokeHeaderFilter(inv);
        return inv;
    }

    /**
     * Intercept the header to do some validation
     * <p>
     * for example, check the token or a user-defined permission check operation
     *
     * @param inv RPC Invocation
     * @throws RpcException maybe throw rpcException
     */
    protected void invokeHeaderFilter(RpcInvocation inv) throws RpcException {
        for (HeaderFilter headerFilter : getHeaderFilters()) {
            headerFilter.invoke(getInvoker(), inv);
        }
    }

    /**
     * For the unary method, there may be overloaded methods,
     * so need to parse out the Wrapper from the data and continue buildRpcInvocation
     * <p>
     * Also, to prevent serialization attacks, headerFilter needs to be executed
     *
     * @param metadata request headers
     * @param data     request data
     * @return RPC Invocation
     */
    protected RpcInvocation buildUnaryInvocation(Metadata metadata, byte[] data) {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (getProviderModel() != null) {
                ClassLoadUtil.switchContextLoader(getProviderModel().getServiceInterfaceClass().getClassLoader());
            }
            // For the Wrapper method,the methodDescriptor needs to get from data, so parse the request first
            if (needDeserializeWrapper(getMethodDescriptor())) {
                // the wrapper structure is first resolved without actual deserialization
                TripleWrapper.TripleRequestWrapper wrapper = deserializeWrapperSetMdIfNeed(data);
                if (wrapper == null) {
                    return null;
                }
                RpcInvocation inv = buildInvocation(metadata);
                inv.setArguments(unwrapReq(url(), wrapper, getMultipleSerialization()));
                return inv;
            } else {
                // Protobuf MethodDescriptor must not be null
                RpcInvocation inv = buildInvocation(metadata);
                inv.setArguments(new Object[]{unpack(data, getMethodDescriptor().getParameterClasses()[0])});
                return inv;
            }
        } catch (RpcException rpcException) {
            // for catch exceptions in headerFilter
            transportError(GrpcStatus.getStatus(rpcException, rpcException.getMessage()));
            return null;
        } catch (Throwable throwable) {
            LOGGER.warn("Decode request failed:", throwable);
            transportError(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                .withDescription("Decode request failed:" + throwable.getMessage()));
            return null;
        } finally {
            ClassLoadUtil.switchContextLoader(tccl);
        }
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
    private Metadata getTrailers(GrpcStatus grpcStatus) {
        Metadata metadata = new DefaultMetadata();
        String grpcMessage = getGrpcMessage(grpcStatus);
        grpcMessage = GrpcStatus.encodeMessage(grpcMessage);
        metadata.put(TripleHeaderEnum.MESSAGE_KEY.getHeader(), grpcMessage);
        metadata.put(TripleHeaderEnum.STATUS_KEY.getHeader(), String.valueOf(grpcStatus.code.code));
        Status.Builder builder = Status.newBuilder()
            .setCode(grpcStatus.code.code)
            .setMessage(grpcMessage);
        Throwable throwable = grpcStatus.cause;
        if (throwable == null) {
            Status status = builder.build();
            metadata.put(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
                encodeBase64ASCII(status.toByteArray()));
            return metadata;
        }
        DebugInfo debugInfo = DebugInfo.newBuilder()
            .addAllStackEntries(ExceptionUtils.getStackFrameList(throwable, 10))
            // can not use now
            // .setDetail(throwable.getMessage())
            .build();
        builder.addDetails(Any.pack(debugInfo));
        Status status = builder.build();
        metadata.put(TripleHeaderEnum.STATUS_DETAIL_KEY.getHeader(),
            encodeBase64ASCII(status.toByteArray()));
        return metadata;
    }


    /**
     * default header
     * <p>
     * only status and content-type
     */
    protected Metadata createDefaultMetadata() {
        Metadata metadata = new DefaultMetadata();
        metadata.put(Http2Headers.PseudoHeaderName.STATUS.value(), HttpResponseStatus.OK.codeAsText());
        metadata.put(HttpHeaderNames.CONTENT_TYPE, TripleConstant.CONTENT_PROTO);
        return metadata;
    }


    private String getGrpcMessage(GrpcStatus status) {
        if (StringUtils.isNotEmpty(status.description)) {
            return status.description;
        }
        if (status.cause != null) {
            return status.cause.getMessage();
        }
        return "unknown";
    }


    public void invoke() {
        RpcInvocation invocation = buildUnaryInvocation(getHeaders(), getData());
        if (invocation == null) {
            return;
        }
        final long stInNano = System.nanoTime();
        final Result result = getInvoker().invoke(invocation);
        CompletionStage<Object> future = result.thenApply(Function.identity());
        future.whenComplete((o, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Invoke error", throwable);
                transportError(getStatus(throwable));
                return;
            }
            AppResponse response = (AppResponse) o;
            if (response.hasException()) {
                transportError(getStatus(response.getException()));
                return;
            }
            final Object timeoutVal = invocation.getObjectAttachment(TIMEOUT_KEY);
            final long cost = System.nanoTime() - stInNano;
            if (timeoutVal != null && cost > ((Long) timeoutVal)) {
                LOGGER.error(String.format("Invoke timeout at server side, ignored to send response. service=%s method=%s cost=%s timeout=%s",
                    invocation.getTargetServiceUniqueName(),
                    invocation.getMethodName(),
                    cost, timeoutVal));
                outboundTransportObserver()
                    .onError(GrpcStatus.fromCode(GrpcStatus.Code.DEADLINE_EXCEEDED));
            } else {
                Metadata metadata = createResponseMeta();
                outboundTransportObserver().onMetadata(metadata, false);
                final byte[] data = encodeResponse(response.getValue());
                if (data == null) {
                    // already handled in encodeResponse()
                    return;
                }
                outboundTransportObserver().onData(data, false);
                Metadata trailers = TripleConstant.getSuccessResponseMeta();
                convertAttachment(trailers, response.getObjectAttachments());
                outboundTransportObserver().onMetadata(trailers, true);
            }
        });
        RpcContext.removeContext();
    }


}
