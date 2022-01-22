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
package org.apache.dubbo.spring.boot.context.event;

import org.apache.dubbo.common.lang.ShutdownHookCallbacks;
import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.util.ClassUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.springframework.util.ObjectUtils.containsElement;

/**
 * Awaiting Non-Web Spring Boot {@link ApplicationListener}
 *
 * @since 2.7.0
 */
public class AwaitingNonWebApplicationListener implements SmartApplicationListener {

    private static final String[] WEB_APPLICATION_CONTEXT_CLASSES = new String[]{
            "org.springframework.web.context.WebApplicationContext",
            "org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext"
    };

    private static final Logger logger = LoggerFactory.getLogger(AwaitingNonWebApplicationListener.class);

    private static final Class<? extends ApplicationEvent>[] SUPPORTED_APPLICATION_EVENTS =
            of(ApplicationReadyEvent.class, ContextClosedEvent.class);

    private final AtomicBoolean awaited = new AtomicBoolean(false);

    private static final Integer UNDEFINED_ID = Integer.valueOf(-1);

    /**
     * Target the application id
     */
    private final AtomicInteger applicationContextId = new AtomicInteger(UNDEFINED_ID);

    private final Lock lock = new ReentrantLock();

    private final Condition condition = lock.newCondition();

    private final ExecutorService executorService = newSingleThreadExecutor();

    private static <T> T[] of(T... values) {
        return values;
    }

    AtomicBoolean getAwaited() {
        return awaited;
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return containsElement(SUPPORTED_APPLICATION_EVENTS, eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            onApplicationReadyEvent((ApplicationReadyEvent) event);
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    protected void onApplicationReadyEvent(ApplicationReadyEvent event) {

        final ConfigurableApplicationContext applicationContext = event.getApplicationContext();

        if (!isRootApplicationContext(applicationContext) || isWebApplication(applicationContext)) {
            return;
        }

        if (applicationContextId.compareAndSet(UNDEFINED_ID, applicationContext.hashCode())) {
            await();
            releaseOnExit(event.getApplicationContext());
        }
    }

    /**
     * @since 2.7.8
     * @param applicationContext
     */
    private void releaseOnExit(ConfigurableApplicationContext applicationContext) {
        ApplicationModel applicationModel = DubboBeanUtils.getApplicationModel(applicationContext);
        if (applicationModel == null) {
            return;
        }
        ShutdownHookCallbacks shutdownHookCallbacks = applicationModel.getBeanFactory().getBean(ShutdownHookCallbacks.class);
        if (shutdownHookCallbacks != null) {
            shutdownHookCallbacks.addCallback(this::release);
        }
    }

    private boolean isRootApplicationContext(ApplicationContext applicationContext) {
        return applicationContext.getParent() == null;
    }

    private boolean isWebApplication(ApplicationContext applicationContext) {
        boolean webApplication = false;
        for (String contextClass : WEB_APPLICATION_CONTEXT_CLASSES) {
            if (isAssignable(contextClass, applicationContext.getClass(), applicationContext.getClassLoader())) {
                webApplication = true;
                break;
            }
        }
        return webApplication;
    }

    private static boolean isAssignable(String target, Class<?> type, ClassLoader classLoader) {
        try {
            return ClassUtils.resolveClassName(target, classLoader).isAssignableFrom(type);
        } catch (Throwable ex) {
            return false;
        }
    }

    protected void await() {

        // has been waited, return immediately
        if (awaited.get()) {
            return;
        }

        if (!executorService.isShutdown()) {
            executorService.execute(() -> executeMutually(() -> {
                while (!awaited.get()) {
                    if (logger.isInfoEnabled()) {
                        logger.info(" [Dubbo] Current Spring Boot Application is await...");
                    }
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }));
        }
    }

    protected void release() {
        executeMutually(() -> {
            while (awaited.compareAndSet(false, true)) {
                if (logger.isInfoEnabled()) {
                    logger.info(" [Dubbo] Current Spring Boot Application is about to shutdown...");
                }
                condition.signalAll();
                // @since 2.7.8 method shutdown() is combined into the method release()
                shutdown();
            }
        });
    }

    private void shutdown() {
        if (!executorService.isShutdown()) {
            // Shutdown executorService
            executorService.shutdown();
        }
    }

    private void executeMutually(Runnable runnable) {
        try {
            lock.lock();
            runnable.run();
        } finally {
            lock.unlock();
        }
    }
}
