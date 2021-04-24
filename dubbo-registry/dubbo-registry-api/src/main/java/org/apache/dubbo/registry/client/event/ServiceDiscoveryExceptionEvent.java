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
package org.apache.dubbo.registry.client.event;

import org.apache.dubbo.registry.client.ServiceDiscovery;

/**
 * An event raised when the {@link ServiceDiscovery Service Discovery} met with some exception
 *
 * @see ServiceDiscovery
 * @see org.apache.dubbo.event.Event
 * @since 2.7.5
 */
public class ServiceDiscoveryExceptionEvent extends ServiceDiscoveryEvent {

    private final Throwable cause;

    public ServiceDiscoveryExceptionEvent(ServiceDiscovery source, ServiceDiscovery original, Throwable cause) {
        super(source, original);
        if (cause == null) {
            throw new NullPointerException("The cause of Exception must not null");
        }
        this.cause = cause;
    }

    /**
     * The cause of {@link Throwable}
     *
     * @return non-nul
     */
    public Throwable getCause() {
        return cause;
    }
}
