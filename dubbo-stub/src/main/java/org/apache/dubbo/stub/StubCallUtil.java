package org.apache.dubbo.stub;

import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;

import java.util.function.Function;

public class StubCallUtil {
    public static RpcException unimplementedMethodException(StubMethodDescriptor methodDescriptor) {
        return TriRpcStatus.UNIMPLEMENTED
                .withDescription(String.format("Method %s is unimplemented",
                        methodDescriptor.fullMethodName))
                .asException();
    }
    public static <T, R> StreamObserver<T> callMethod(StubMethodDescriptor methodDescriptor,
                                                      StreamObserver<R> responseObserver,
                                                      Function<T, R> function
    ) {
        return new StreamObserver<T>() {
            @Override
            public void onNext(T data) {
                try {
                    responseObserver.onNext(function.apply(data));
                    responseObserver.onCompleted();
                } catch (Throwable t) {
                    responseObserver.onError(TriRpcStatus.INTERNAL
                            .withDescription(String.format("Call %s error", methodDescriptor.fullMethodName))
                            .asException());
                }
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onCompleted() {
            }
        };
    }

}
