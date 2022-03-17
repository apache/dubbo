package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.HeaderFilter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.FrameworkServiceRepository;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.StreamMethodDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.ClassLoadUtil;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericPack;
import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ServerStreamListener;
import org.apache.dubbo.rpc.service.ServiceDescriptorInternalCache;
import org.apache.dubbo.triple.TripleWrapper;

import com.google.protobuf.Message;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class ReflectionServerCall extends ServerCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionServerCall.class);
    private final List<HeaderFilter> headerFilters;
    private final GenericUnpack genericUnpack;
    private MethodDescriptor methodDescriptor;
    private List<MethodDescriptor> methodDescriptors;
    private StubMethodDescriptor.UnPack unpack;
    private RpcInvocation invocation;
    private String serializeType;

    public ReflectionServerCall(Invoker<?> invoker,
                                ServerStream serverStream,
                                FrameworkModel frameworkModel,
                                String serviceName,
                                String methodName,
                                List<HeaderFilter> headerFilters,
                                Executor executor, GenericUnpack genericUnpack) {
        super(invoker, serverStream, frameworkModel, serviceName, methodName, executor);
        this.genericUnpack = genericUnpack;
        this.headerFilters = headerFilters;
    }

    private boolean isEcho(String methodName) {
        return CommonConstants.$ECHO.equals(methodName);
    }

    private boolean isGeneric(String methodName) {
        return CommonConstants.$INVOKE.equals(methodName) || CommonConstants.$INVOKE_ASYNC.equals(methodName);
    }

    @Override
    public ServerStreamListener doStartCall(Map<String, Object> metadata) {
        FrameworkServiceRepository repo = frameworkModel.getServiceRepository();
        ProviderModel providerModel = repo.lookupExportedService(invoker.getUrl().getServiceKey());
        if (providerModel == null || providerModel.getServiceModel() == null) {
            responseErr(TriRpcStatus.UNIMPLEMENTED
                    .withDescription("Service not found:" + serviceName));
            return null;
        }
        serviceDescriptor = providerModel.getServiceModel();

        if (isGeneric(methodName)) {
            // There should be one and only one
            methodDescriptor = ServiceDescriptorInternalCache.genericService().getMethods(methodName).get(0);
        } else if (isEcho(methodName)) {
            // There should be one and only one
            methodDescriptor = ServiceDescriptorInternalCache.echoService().getMethods(methodName).get(0);
        } else {
            methodDescriptors = serviceDescriptor.getMethods(methodName);
            // try upper-case method
            if (CollectionUtils.isEmpty(methodDescriptors)) {
                final String upperMethod = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
                methodDescriptors = serviceDescriptor.getMethods(upperMethod);
            }
            if (CollectionUtils.isEmpty(methodDescriptors)) {
                responseErr(TriRpcStatus.UNIMPLEMENTED
                        .withDescription("Method : " + methodName + " not found of service:" + serviceName));
                return null;
            }
            // In most cases there is only one method
            if (methodDescriptors.size() == 1) {
                methodDescriptor = methodDescriptors.get(0);
            }
        }
        ServerStreamListenerImpl listener = new ServerStreamListenerImpl();
        listener.startCall(metadata);
        return listener;
    }

    @Override
    protected byte[] packResponse(Object message) throws IOException {
        return ((Message)message).toByteArray();
    }


    class ServerStreamListenerImpl extends ServerCall.ServerStreamListenerBase {

        private Map<String, Object> metadata;

        void startCall(Map<String, Object> metadata) {
            this.metadata = metadata;
            trySetUnpack();
            trySetListener();
            if (listener == null) {
                // wrap request , need one message
                requestN(1);
            }
        }

        @Override
        public void complete() {
            if (listener != null) {
                listener.onComplete();
            }
        }

        @Override
        public void cancel(TriRpcStatus status) {
            listener.onCancel(status.description);
        }

        @Override
        protected void doOnMessage(byte[] message) throws IOException {
            trySetMethodDescriptor(message);
            trySetUnpack();
            trySetListener();
            if(closed){
                return;
            }
            if (serviceDescriptor != null) {
                ClassLoadUtil.switchContextLoader(serviceDescriptor.getServiceInterfaceClass().getClassLoader());
            }
            final Object obj = unpack.unpack(message);
            listener.onMessage(obj);
        }

        private void trySetUnpack() {
            if (methodDescriptor == null) {
                return;
            }
            if (unpack != null) {
                return;
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
        }

        private void trySetMethodDescriptor(byte[] data) throws IOException {
            if (methodDescriptor != null) {
                return;
            }
            final TripleWrapper.TripleRequestWrapper request;
            request = TripleWrapper.TripleRequestWrapper.parseFrom(data);

            serializeType = request.getSerializeType();

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
                close(TriRpcStatus.UNIMPLEMENTED
                        .withDescription("Method :" + methodName + "[" + Arrays.toString(paramTypes) + "] " +
                                "not found of service:" + serviceDescriptor.getInterfaceName()), null);
            }
        }

        private void trySetListener() {
            if (listener != null) {
                return;
            }
            if (methodDescriptor == null) {
                return;
            }
            if (closed) {
                return;
            }
            invocation = buildInvocation(metadata);
            if (closed) {
                return;
            }
            headerFilters.forEach(f -> f.invoke(invoker, invocation));
            if (closed) {
                return;
            }
            GenericPack genericPack = new GenericPack(genericUnpack.serialization, serializeType, invoker.getUrl());
            listener = ServerCallUtil.startCall(ReflectionServerCall.this,
                    invocation,
                    methodDescriptor,
                    genericUnpack,
                    genericPack,
                    invoker);
            if (listener == null) {
                closed = true;
            }
        }

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
                methodName, serviceDescriptor.getInterfaceName(),
                url.getProtocolServiceKey(), methodDescriptor.getParameterClasses(), new Object[0]);
        inv.setTargetServiceUniqueName(url.getServiceKey());
        inv.setReturnTypes(methodDescriptor.getReturnTypes());
        inv.setObjectAttachments(headers);
        return inv;
    }
}

