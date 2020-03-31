package org.apache.dubbo.common.threadpool.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *  {@link ThreadPoolExhaustedEvent} Test
 */
public class ThreadPoolExhaustedEventTest {

    @Test
    public void test() {
        long timestamp = System.currentTimeMillis();
        String msg = "Thread pool is EXHAUSTED! Thread Name: DubboServerHandler-127.0.0.1:12345, Pool Size: 1 (active: 0, core: 1, max: 1, largest: 1), Task: 6 (completed: 6), Executor status:(isShutdown:false, isTerminated:false, isTerminating:false), in dubbo://127.0.0.1:12345!, dubbo version: 2.7.3, current host: 127.0.0.1";
        ThreadPoolExhaustedEvent event = new ThreadPoolExhaustedEvent(this, msg);

        assertEquals(this, event.getSource());
        assertEquals(msg, event.getMsg());
        assertTrue(event.getTimestamp() >= timestamp);
    }
}
