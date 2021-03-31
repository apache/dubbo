package org.apache.dubbo.common.stream;

public interface StreamObserver<T> {
    void onNext(T data);

    void onError(Throwable throwable);

    void onCompleted();
}
