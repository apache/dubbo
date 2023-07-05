/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.metrics.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 */
public class MetricsEventBus {

    /**
     * Posts an event to all registered subscribers and only once.
     *
     * @param event event to post.
     */
    public static void publish(MetricsEvent event) {
        if (event.getSource() == null) {
            return;
        }
        ApplicationModel applicationModel = event.getSource();
        if (applicationModel.isDestroyed()) {
            return;
        }
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        if (beanFactory.isDestroyed()) {
            return;
        }
        MetricsDispatcher dispatcher = beanFactory.getBean(MetricsDispatcher.class);
        Optional.ofNullable(dispatcher).ifPresent(d -> d.publishEvent(event));
    }

    /**
     * Posts an event to all registered subscribers.
     * Full lifecycle post, judging success or failure by whether there is an exception
     * Loop around the event target and return the original processing result
     *
     * @param event          event to post.
     * @param targetSupplier original processing result targetSupplier
     */
    public static <T> T post(MetricsEvent event, Supplier<T> targetSupplier) {
        return post(event, targetSupplier, null);

    }

    /**
     * Full lifecycle post, success and failure conditions can be customized
     *
     * @param event          event to post.
     * @param targetSupplier original processing result supplier
     * @param trFunction     Custom event success criteria, judged according to the returned boolean type
     * @param <T>            Biz result type
     * @return Biz result
     */
    public static <T> T post(MetricsEvent event, Supplier<T> targetSupplier, Function<T, Boolean> trFunction) {
        T result;
        before(event);
        if (trFunction == null) {
            try {
                result = targetSupplier.get();
            } catch (Throwable e) {
                error(event);
                throw e;
            }
            after(event, result);
        } else {
            // Custom failure status
            result = targetSupplier.get();
            if (trFunction.apply(result)) {
                after(event, result);
            } else {
                error(event);
            }
        }
        return result;
    }

    public static void before(MetricsEvent event) {
        before(event, null);
    }

    /**
     * Applicable to the scene where execution and return are separated,
     * eventSaveRunner saves the event, so that the calculation rt is introverted
     */
    public static void before(MetricsEvent event, Runnable eventSaveRunner) {
        MetricsDispatcher dispatcher = validate(event);
        if (dispatcher == null) return;
        dispatcher.publishEvent(event);
        if (eventSaveRunner != null) {
            eventSaveRunner.run();
        }
    }

    public static void after(MetricsEvent event, Object result) {
        MetricsDispatcher dispatcher = validate(event);
        if (dispatcher == null) return;
        event.customAfterPost(result);
        dispatcher.publishFinishEvent((TimeCounterEvent) event);
    }

    public static void error(MetricsEvent event) {
        MetricsDispatcher dispatcher = validate(event);
        if (dispatcher == null) return;
        dispatcher.publishErrorEvent((TimeCounterEvent) event);
    }

    private static MetricsDispatcher validate(MetricsEvent event) {
        if (!(event instanceof TimeCounterEvent)) {
            return null;
        }
        if (event.getSource() == null) {
            return null;
        }
        ApplicationModel applicationModel = event.getSource();
        if (applicationModel.isDestroyed()) {
            return null;
        }
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        if (beanFactory.isDestroyed()) {
            return null;
        }
        MetricsDispatcher dispatcher = beanFactory.getBean(MetricsDispatcher.class);

        return dispatcher;
    }
}
