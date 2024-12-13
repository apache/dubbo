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

import org.apache.dubbo.config.spring.util.DubboBeanUtils;
import org.apache.dubbo.remoting.http12.rest.OpenAPIService;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * OpenAPI Export Listener
 * <p>
 * This class exports OpenAPI specifications for Dubbo services
 * when the Spring Boot application is fully started and ready.
 */
public class DubboOpenAPIExportListener implements ApplicationListener<ApplicationReadyEvent> {

    private final AtomicBoolean exported = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (!exported.compareAndSet(false, true)) {
            return;
        }
        ApplicationModel applicationModel = DubboBeanUtils.getApplicationModel(event.getApplicationContext());
        if (applicationModel == null) {
            return;
        }
        OpenAPIService openAPIService = applicationModel.getBean(OpenAPIService.class);
        if (openAPIService != null) {
            openAPIService.export();
        }
    }
}
