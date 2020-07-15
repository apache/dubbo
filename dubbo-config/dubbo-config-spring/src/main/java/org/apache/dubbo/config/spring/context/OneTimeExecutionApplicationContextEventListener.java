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
package org.apache.dubbo.config.spring.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

import java.util.Objects;

/**
 * The abstract class {@link ApplicationListener} for {@link ApplicationContextEvent} guarantees just one-time execution
 * and prevents the event propagation in the hierarchical {@link ApplicationContext ApplicationContexts}
 *
 * @since 2.7.5
 */
abstract class OneTimeExecutionApplicationContextEventListener implements ApplicationListener, ApplicationContextAware {

    private ApplicationContext applicationContext;

    public final void onApplicationEvent(ApplicationEvent event) {
        if (isOriginalEventSource(event) && event instanceof ApplicationContextEvent) {
            onApplicationContextEvent((ApplicationContextEvent) event);
        }
    }

    /**
     * The subclass overrides this method to handle {@link ApplicationContextEvent}
     *
     * @param event {@link ApplicationContextEvent}
     */
    protected abstract void onApplicationContextEvent(ApplicationContextEvent event);

    /**
     * Is original {@link ApplicationContext} as the event source
     *
     * @param event {@link ApplicationEvent}
     * @return
     */
    private boolean isOriginalEventSource(ApplicationEvent event) {
        return (applicationContext == null) // Current ApplicationListener is not a Spring Bean, just was added
                // into Spring's ConfigurableApplicationContext
                || Objects.equals(applicationContext, event.getSource());
    }

    @Override
    public final void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
