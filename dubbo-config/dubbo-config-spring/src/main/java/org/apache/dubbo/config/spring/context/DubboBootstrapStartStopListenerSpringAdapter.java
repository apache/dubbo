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

import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.config.bootstrap.DubboBootstrapStartStopListener;
import org.apache.dubbo.config.spring.context.event.DubboBootstrapStatedEvent;
import org.apache.dubbo.config.spring.context.event.DubboBootstrapStopedEvent;

import org.springframework.context.ApplicationContext;

/**
 * convcert Dubbo bootstrap event to spring environment.
 *
 * @scene 2.7.9
 */
public class DubboBootstrapStartStopListenerSpringAdapter implements DubboBootstrapStartStopListener {

    static ApplicationContext applicationContext;

    @Override
    public void onStart(DubboBootstrap bootstrap) {
        if (applicationContext != null) {
            applicationContext.publishEvent(new DubboBootstrapStatedEvent(bootstrap));
        }
    }

    @Override
    public void onStop(DubboBootstrap bootstrap) {
        if (applicationContext != null) {
            applicationContext.publishEvent(new DubboBootstrapStopedEvent(bootstrap));
        }
    }
}
