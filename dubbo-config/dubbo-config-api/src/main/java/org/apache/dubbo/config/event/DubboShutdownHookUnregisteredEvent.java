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
package org.apache.dubbo.config.event;

import org.apache.dubbo.config.DubboShutdownHook;
import org.apache.dubbo.event.Event;

/**
 * An event raised when the {@link DubboShutdownHook} {@link Runtime#removeShutdownHook(Thread) unregistered} on
 * {@link ApplicationShutdownHooks JVM ShutdownHooks}
 *
 * @see DubboShutdownHook
 * @see ApplicationShutdownHooks
 * @since 2.7.5
 */
public class DubboShutdownHookUnregisteredEvent extends Event {

    /**
     * Constructs a prototypical Event.
     *
     * @param dubboShutdownHook The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public DubboShutdownHookUnregisteredEvent(DubboShutdownHook dubboShutdownHook) {
        super(dubboShutdownHook);
    }

    public DubboShutdownHook getDubboShutdownHook() {
        return (DubboShutdownHook) getSource();
    }
}
