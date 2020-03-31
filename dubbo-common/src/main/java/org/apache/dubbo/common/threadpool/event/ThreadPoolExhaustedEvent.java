package org.apache.dubbo.common.threadpool.event;

import org.apache.dubbo.event.Event;

/**
 * An {@link Event Dubbo event} when the Dubbo thread pool is exhausted.
 *
 * @see Event
 */
public class ThreadPoolExhaustedEvent extends Event {

    final String msg;

    public ThreadPoolExhaustedEvent(Object source, String msg) {
        super(source);
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
