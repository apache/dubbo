package org.apache.dubbo.metrics.event;

import org.apache.dubbo.metrics.listener.MetricsLifeListener;
import org.apache.dubbo.metrics.listener.MetricsListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SimpleMetricsEventMulticasterTest {

    private SimpleMetricsEventMulticaster eventMulticaster;
    private Object[] obj;
    MetricsEvent<Object> requestEvent;

    @BeforeEach
    public void setup() {
        eventMulticaster = new SimpleMetricsEventMulticaster();
        obj = new Object[]{new Object()};
        eventMulticaster.addListener((MetricsListener<MetricsEvent<Object>>) event -> obj[0] = new Object());
        requestEvent = new RequestEvent(obj[0], MetricsEvent.Type.TOTAL);

    }


    @Test
    void testPublishEvent() {

        // emptyEvent do nothing
        MetricsEvent<Object> emptyEvent = new EmptyEvent(obj[0]);
        eventMulticaster.publishEvent(emptyEvent);
        Assertions.assertSame(emptyEvent.getSource(), obj[0]);

        // not empty Event change obj[]
        MetricsEvent<Object> requestEvent = new RequestEvent(obj[0], MetricsEvent.Type.TOTAL);
        eventMulticaster.publishEvent(requestEvent);
        Assertions.assertNotSame(requestEvent.getSource(), obj[0]);

    }

    @Test
    void testPublishFinishEvent() {

        //do nothing with no MetricsLifeListener
        eventMulticaster.publishFinishEvent(requestEvent);
        Assertions.assertSame(requestEvent.getSource(), obj[0]);

        //do onEventFinish with MetricsLifeListener
        eventMulticaster.addListener((new MetricsLifeListener<MetricsEvent<Object>>() {

            @Override
            public void onEvent(MetricsEvent<Object> event) {

            }

            @Override
            public void onEventFinish(MetricsEvent<Object> event) {
                obj[0] = new Object();
            }

            @Override
            public void onEventError(MetricsEvent<Object> event) {

            }
        }));
        eventMulticaster.publishFinishEvent(requestEvent);
        Assertions.assertNotSame(requestEvent.getSource(), obj[0]);

    }

    @Test
    void testPublishErrorEvent() {

    }
}
