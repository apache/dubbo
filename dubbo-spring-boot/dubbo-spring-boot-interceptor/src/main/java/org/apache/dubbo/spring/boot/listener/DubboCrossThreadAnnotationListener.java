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
package org.apache.dubbo.spring.boot.listener;

import java.lang.instrument.Instrumentation;
import org.apache.dubbo.spring.boot.interceptor.RunnableOrCallableActivation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

public class DubboCrossThreadAnnotationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    private Logger logger = LoggerFactory.getLogger(DubboCrossThreadAnnotationListener.class);
    private Instrumentation instrumentation;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
        RunnableOrCallableActivation.install(this.instrumentation);
        logger.info("finished byte buddy installation.");
    }

    public DubboCrossThreadAnnotationListener(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    private DubboCrossThreadAnnotationListener() {

    }
}
