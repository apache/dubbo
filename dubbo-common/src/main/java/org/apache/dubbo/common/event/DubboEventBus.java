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
package org.apache.dubbo.common.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_METRICS_COLLECTOR_EXCEPTION;

/**
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 *
 * @see DubboEvent
 * @see DubboListener
 * @see DubboLifecycleEventMulticaster
 * @since 3.3.0
 */
public class DubboEventBus {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DubboEventBus.class);

    private static final ConcurrentHashMap<ApplicationModel, DubboLifecycleEventMulticaster> cachedMulticasterMap =
            new ConcurrentHashMap<>();

    private DubboEventBus() {}

    /**
     * Registers all subscriber methods on {@code object} to receive events.
     *
     * @param listener object whose subscriber methods should be registered.
     */
    public static void addListener(ApplicationModel applicationModel, DubboListener<?> listener) {
        getMulticaster(applicationModel).addListener(listener);
    }

    /**
     * Unregisters all subscriber methods on a registered {@code object}.
     *
     * @param listener object whose subscriber methods should be unregistered.
     * @throws IllegalArgumentException if the object was not previously registered.
     */
    public static void removeListener(ApplicationModel applicationModel, DubboListener<?> listener) {
        getMulticaster(applicationModel).removeListener(listener);
    }

    /**
     * Posts an event to all registered subscribers and only once.
     *
     * @param event event to post.
     */
    public static void publish(DubboEvent event) {
        if (event.getSource() == null) {
            return;
        }
        tryInvoke(() -> getMulticaster(event.getApplicationModel()).publishEvent(event));
    }

    /**
     * Posts an event to all registered subscribers.
     * Full lifecycle post, judging success or failure by whether there is an exception
     * Loop around the event target and return the original processing result
     *
     * @param event          event to post.
     * @param targetSupplier original processing result targetSupplier
     */
    public static <T> T post(DubboEvent event, Supplier<T> targetSupplier) {
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
    public static <T> T post(DubboEvent event, Supplier<T> targetSupplier, Function<T, Boolean> trFunction) {
        T result;
        tryInvoke(() -> before(event));
        try {
            result = targetSupplier.get();
        } catch (Throwable e) {
            tryInvoke(() -> error(event));
            throw e;
        }
        if (trFunction == null) {
            tryInvoke(() -> after(event, result));
            return result;
        }
        // Custom failure status
        if (trFunction.apply(result)) {
            tryInvoke(() -> after(event, result));
        } else {
            tryInvoke(() -> error(event));
        }
        return result;
    }

    private static void tryInvoke(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            logger.warn(COMMON_METRICS_COLLECTOR_EXCEPTION, "", "", "invoke metric event error" + e.getMessage());
        }
    }

    /**
     * Applicable to the scene where execution and return are separated,
     * eventSaveRunner saves the event, so that the calculation rt is introverted
     */
    public static void before(DubboEvent event) {
        tryInvoke(() -> getMulticaster(event.getApplicationModel()).publishBeforeEvent(event));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void after(DubboEvent event, Object result) {
        tryInvoke(() -> {
            if (event instanceof CustomAfterPost) {
                ((CustomAfterPost) event).customAfterPost(result);
            }
            getMulticaster(event.getApplicationModel()).publishEvent(event);
        });
    }

    public static void error(DubboEvent event) {
        tryInvoke(() -> getMulticaster(event.getApplicationModel()).publishErrorEvent(event));
    }

    private static DubboLifecycleEventMulticaster getMulticaster(ApplicationModel applicationModel) {
        return cachedMulticasterMap.computeIfAbsent(applicationModel, t -> {
            ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
            return beanFactory.getBean(DubboLifecycleEventMulticaster.class);
        });
    }
}
