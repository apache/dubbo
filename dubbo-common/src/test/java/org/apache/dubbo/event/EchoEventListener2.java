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
package org.apache.dubbo.event;

import java.io.Serializable;
import java.util.Objects;
import java.util.Vector;

/**
 * {@link EchoEvent} {@link EventListener} 2
 *
 * @since 2.7.5
 */
public class EchoEventListener2 extends Vector<EventListener<Event>> implements Serializable, EventListener<Event> {

    private AbstractEventListener<Event> delegate = new AbstractEventListener<Event>() {
        @Override
        protected void handleEvent(Event event) {
            println("EchoEventListener2 : " + event);
        }
    };

    @Override
    public void onEvent(Event event) {
        delegate.onEvent(event);
    }

    @Override
    public int getPriority() {
        return -1;
    }

    public int getEventOccurs() {
        return delegate.getEventOccurs();
    }

    @Override
    public boolean equals(Object o) {
        return this.getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClass());
    }
}
