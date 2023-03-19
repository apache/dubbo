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
public class EventBus {

    /**
     * Posts an event to all registered subscribers.
     *
     * @param event event to post.
     */
    public static void solo(MetricsEvent event) {
        if (event.getSource() instanceof ApplicationModel) {
            ApplicationModel applicationModel = (ApplicationModel) event.getSource();
            if (applicationModel.isDestroyed()) {
                return;
            }
            ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
            if (beanFactory.isDestroyed()) {
                return;
            }
            Dispatcher dispatcher = beanFactory.getBean(Dispatcher.class);
            Optional.ofNullable(dispatcher).ifPresent(d -> d.publishEvent(event));
        }
    }

    /**
     * Posts an event to all registered subscribers.
     * Loop around the event target and return the original processing result
     *
     * @param event    event to post.
     * @param supplier original processing result supplier
     */
    public static <T> T post(MetricsEvent event, Supplier<T> supplier) {
        return post(event, supplier, null);

    }

    /**
     * Same as above, the difference is that you can customize success/failure
     *
     * @param event      event to post.
     * @param supplier   original processing result supplier
     * @param trFunction Custom event success criteria, judged according to the returned boolean type
     * @param <T>        Biz result type
     * @return Biz result
     */
    public static <T> T post(MetricsEvent event, Supplier<T> supplier, Function<T, Boolean> trFunction) {
        if (!(event.getSource() instanceof ApplicationModel)) {
            return supplier.get();
        }
        ApplicationModel applicationModel = (ApplicationModel) event.getSource();
        if (applicationModel.isDestroyed()) {
            return supplier.get();
        }
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        if (beanFactory.isDestroyed()) {
            return supplier.get();
        }
        Dispatcher dispatcher = beanFactory.getBean(Dispatcher.class);
        if (dispatcher == null) {
            return supplier.get();
        }
        dispatcher.publishEvent(event);
        T result;
        if (trFunction == null) {
            try {
                result = supplier.get();
            } catch (Throwable e) {
                dispatcher.publishErrorEvent(event);
                throw e;
            }
            dispatcher.publishFinishEvent(event);
        } else {
            result = supplier.get();
            if (trFunction.apply(result)) {
                dispatcher.publishFinishEvent(event);
            } else {
                dispatcher.publishErrorEvent(event);
            }
        }
        return result;
    }
}
