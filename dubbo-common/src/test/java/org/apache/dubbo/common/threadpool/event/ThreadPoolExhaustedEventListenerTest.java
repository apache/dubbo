package org.apache.dubbo.common.threadpool.event;

import org.apache.dubbo.event.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ThreadPoolExhaustedEvent} Test
 */
public class ThreadPoolExhaustedEventListenerTest {

    private EventDispatcher eventDispatcher;

    private ThreadPoolExhaustedEventListenerTest.MyGenericEventListener listener;

    @BeforeEach
    public void init() {
        this.listener = new ThreadPoolExhaustedEventListenerTest.MyGenericEventListener();
        this.eventDispatcher = EventDispatcher.getDefaultExtension();
        this.eventDispatcher.addEventListener(listener);
    }

    @AfterEach
    public void destroy() {
        this.eventDispatcher.removeAllEventListeners();
    }

    @Test
    public void testOnEvent() {
        String msg = "Thread pool is EXHAUSTED! Thread Name: DubboServerHandler-127.0.0.1:12345, Pool Size: 1 (active: 0, core: 1, max: 1, largest: 1), Task: 6 (completed: 6), Executor status:(isShutdown:false, isTerminated:false, isTerminating:false), in dubbo://127.0.0.1:12345!, dubbo version: 2.7.3, current host: 127.0.0.1";
        ThreadPoolExhaustedEvent exhaustedEvent = new ThreadPoolExhaustedEvent(this, msg);
        eventDispatcher.dispatch(exhaustedEvent);
        assertEquals(exhaustedEvent, listener.getThreadPoolExhaustedEvent());
        assertEquals(this, listener.getThreadPoolExhaustedEvent().getSource());
    }

    class MyGenericEventListener implements EventListener<ThreadPoolExhaustedEvent> {

        private ThreadPoolExhaustedEvent threadPoolExhaustedEvent;

        @Override
        public void onEvent(ThreadPoolExhaustedEvent event) {
            this.threadPoolExhaustedEvent = event;
        }

        public ThreadPoolExhaustedEvent getThreadPoolExhaustedEvent() {
            return threadPoolExhaustedEvent;
        }
    }
}
