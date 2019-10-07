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
package org.apache.dubbo.config.spring.context.event;

import org.apache.dubbo.config.spring.ServiceBean;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * A {@link ApplicationEvent} after {@link ServiceBean} {@link ServiceBean#export() export} invocation
 *
 * @see ApplicationEvent
 * @see ApplicationListener
 * @see ServiceBean
 * @since 2.6.5
 */
public class ServiceBeanExportedEvent extends ApplicationEvent {

    /**
     * Create a new ApplicationEvent.
     *
     * @param serviceBean {@link ServiceBean} bean
     */
    public ServiceBeanExportedEvent(ServiceBean serviceBean) {
        super(serviceBean);
    }

    /**
     * Get {@link ServiceBean} instance
     *
     * @return non-null
     */
    public ServiceBean getServiceBean() {
        return (ServiceBean) super.getSource();
    }
}
