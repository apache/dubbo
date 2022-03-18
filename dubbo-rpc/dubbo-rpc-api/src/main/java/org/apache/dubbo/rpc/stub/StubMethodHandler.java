package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.stream.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface StubMethodHandler<T, R> {
    CompletableFuture<?> invoke(Object[] arguments);
}

