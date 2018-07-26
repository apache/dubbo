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
package org.apache.dubbo.config.spring.initializer;

import org.apache.dubbo.bootstrap.DubboBootstrap;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * An application listener that listens the ContextClosedEvent.
 * Upon the event, this listener will do the necessary clean up to avoid memory leak.
 */
public class DubboApplicationListener implements ApplicationListener<ApplicationEvent> {

    private DubboBootstrap dubboBootstrap;

    public DubboApplicationListener() {
        dubboBootstrap = new DubboBootstrap(false);
    }

    public DubboApplicationListener(DubboBootstrap dubboBootstrap) {
        this.dubboBootstrap = dubboBootstrap;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextRefreshedEvent) {
            dubboBootstrap.start();
        } else if (applicationEvent instanceof ContextClosedEvent) {
            dubboBootstrap.stop();
        }
    }
}
