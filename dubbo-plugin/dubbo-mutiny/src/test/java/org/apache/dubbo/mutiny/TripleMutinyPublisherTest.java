package org.apache.dubbo.mutiny;

import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for AbstractTripleMutinyPublisher
 */
public class TripleMutinyPublisherTest {

    @Test
    public void testSubscribeAndRequest() {
        AtomicBoolean subscribed = new AtomicBoolean(false);

        AbstractTripleMutinyPublisher<String> publisher = new AbstractTripleMutinyPublisher<>() {
            @Override
            protected void onSubscribe(CallStreamObserver<?> subscription) {
                subscribed.set(true);
                this.subscription = Mockito.mock(CallStreamObserver.class);
            }
        };

        publisher.onSubscribe(Mockito.mock(CallStreamObserver.class));

        Flow.Subscriber<String> subscriber = new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(String item) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        };

        publisher.subscribe(subscriber);
        assertTrue(subscribed.get());
    }

    @Test
    public void testRequestBeforeStartRequest() {
        CallStreamObserver<?> mockObserver = Mockito.mock(CallStreamObserver.class);

        AbstractTripleMutinyPublisher<String> publisher = new AbstractTripleMutinyPublisher<>() {};
        publisher.onSubscribe(mockObserver);
        publisher.request(5L); // should accumulate, not call request()
        Mockito.verify(mockObserver, Mockito.never()).request(Mockito.anyInt());

        publisher.startRequest(); // now should flush request
        Mockito.verify(mockObserver).request(5);
    }

    @Test
    public void testCancelTriggersShutdownHook() {
        AtomicBoolean shutdown = new AtomicBoolean(false);

        AbstractTripleMutinyPublisher<String> publisher = new AbstractTripleMutinyPublisher<>(
                null, () -> shutdown.set(true)) {};

        publisher.cancel();
        assertTrue(publisher.isCancelled());
        assertTrue(shutdown.get());
    }

    @Test
    public void testOnNextAndComplete() {
        List<String> received = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();

        AbstractTripleMutinyPublisher<String> publisher = new AbstractTripleMutinyPublisher<>() {};

        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
            }

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }
        });

        publisher.onNext("hello");
        publisher.onNext("world");
        publisher.onCompleted();

        assertEquals(List.of("hello", "world"), received);
        assertTrue(completed.get());
    }
}
