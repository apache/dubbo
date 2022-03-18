package org.apache.dubbo.stub;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class StubCallUtil {
    public static RpcException unimplementedMethodException(StubMethodDescriptor methodDescriptor) {
        return TriRpcStatus.UNIMPLEMENTED
                .withDescription(String.format("Method %s is unimplemented",
                        methodDescriptor.fullMethodName))
                .asException();
    }

    public static <T, R> void callUnaryMethod(T request, StreamObserver<R> responseObserver,
                                              Function<T, R> unaryFunction) {
        R response = unaryFunction.apply(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public static <T, R> CompletableFuture<R> callUnaryMethod(T request, BiConsumer<T, StreamObserver<R>> method) {
        CompletableFuture<R> future = new CompletableFuture<>();
        StreamObserver<R> responseObserver = new FutureToObserverAdaptor<>(future);
        method.accept(request, responseObserver);
        return future;
    }
}
