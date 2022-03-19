package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;
import org.apache.dubbo.rpc.proxy.InvocationUtil;

public class StubInvocationUtil {

    public static <T, R> R unaryCall(Invoker<?> invoker, StubMethodDescriptor methodDescriptor, T request) {
        RpcInvocation rpcInvocation = new RpcInvocation(invoker.getUrl().getServiceModel(),
            methodDescriptor.getMethodName(), invoker.getInterface().getName(),
            invoker.getUrl().getProtocolServiceKey(), methodDescriptor.getParameterClasses(), new Object[]{request});
        try {
            return (R) InvocationUtil.invoke(invoker, rpcInvocation);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    public static <T, R> void unaryCall(Invoker<?> invoker,
        MethodDescriptor method,
        T request,
        StreamObserver<R> responseObserver) {

    }

    public static <T, R> StreamObserver<T> biOrClientStreamCall(Invoker<?> invoker,
        MethodDescriptor method,
        StreamObserver<R> responseObserver) {
        RpcInvocation rpcInvocation = new RpcInvocation(invoker.getUrl().getServiceModel(), method.getMethodName(),
            invoker.getInterface().getName(), invoker.getUrl().getProtocolServiceKey(), method.getParameterClasses(),
            new Object[]{responseObserver});
        try {
            return (StreamObserver<T>) InvocationUtil.invoke(invoker, rpcInvocation);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    public static <T, R> void serverStreamCall(Invoker<?> invoker,
        MethodDescriptor method,
        T request,
        StreamObserver<R> responseObserver) {
        RpcInvocation rpcInvocation = new RpcInvocation(invoker.getUrl().getServiceModel(), method.getMethodName(),
            invoker.getInterface().getName(), invoker.getUrl().getProtocolServiceKey(), method.getParameterClasses(),
            new Object[]{request, responseObserver});
        try {
            InvocationUtil.invoke(invoker, rpcInvocation);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
