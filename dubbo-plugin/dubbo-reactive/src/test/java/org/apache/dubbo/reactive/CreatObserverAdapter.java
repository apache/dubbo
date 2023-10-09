package org.apache.dubbo.reactive;

import org.apache.dubbo.rpc.protocol.tri.observer.ServerCallToObserverAdapter;

import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

public class CreatObserverAdapter {

    private ServerCallToObserverAdapter<String> responseObserver;
    private AtomicInteger nextCounter;
    private AtomicInteger completeCounter;
    private AtomicInteger errorCounter;

    CreatObserverAdapter() {

        nextCounter = new AtomicInteger();
        completeCounter = new AtomicInteger();
        errorCounter = new AtomicInteger();

        responseObserver = Mockito.mock(ServerCallToObserverAdapter.class);
        doAnswer(o -> nextCounter.incrementAndGet())
            .when(responseObserver).onNext(anyString());
        doAnswer(o -> completeCounter.incrementAndGet())
            .when(responseObserver).onCompleted();
        doAnswer(o -> errorCounter.incrementAndGet())
            .when(responseObserver).onError(any(Throwable.class));

    }

    public AtomicInteger getCompleteCounter() {
        return completeCounter;
    }

    public AtomicInteger getNextCounter() {
        return nextCounter;
    }

    public AtomicInteger getErrorCounter() {
        return errorCounter;
    }

    public ServerCallToObserverAdapter<String> getResponseObserver() {
        return this.responseObserver;
    }
}
