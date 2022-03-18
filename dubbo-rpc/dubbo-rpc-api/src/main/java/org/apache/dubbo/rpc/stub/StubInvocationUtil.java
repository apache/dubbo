package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.proxy.InvocationUtil;

public class StubInvocationUtil {

    public static <T, R> R unaryCall(Invoker<?> invoker, StubMethodDescriptor methodDescriptor, T request) {
        RpcInvocation rpcInvocation = new RpcInvocation(invoker.getUrl().getServiceModel(),
            methodDescriptor.getMethodName(),
            invoker.getInterface().getName(),
            invoker.getUrl().getProtocolServiceKey(),
            methodDescriptor.getParameterClasses(),
            new Object[]{request});
        try {
            return (R) InvocationUtil.invoke(invoker, rpcInvocation);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static <T, R> void unaryCall(Invoker<?> invoker,
        StubMethodDescriptor sayHelloMethod,
        T request,
        StreamObserver<R> responseObserver) {

    }
}
