package org.apache.dubbo.common.stream;

public interface StreamObserver<T> {
    void onNext(T var1);

    void onError(Throwable var1);

    void onComplete();
}
