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

import org.apache.dubbo.common.function.ThrowableConsumer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.function.ThrowableFunction.execute;

/**
 * An abstract class of {@link EventListener} for Generic events, the sub class could add more {@link Event event}
 * handle methods, rather than only binds the {@link EventListener#onEvent(Event)} method that is declared to be
 * <code>final</code> the implementation can't override. It's notable that all {@link Event event} handle methods must
 * meet following conditions:
 * <ul>
 * <li>not {@link #onEvent(Event)} method</li>
 * <li><code>public</code> accessibility</li>
 * <li><code>void</code> return type</li>
 * <li>no {@link Exception exception} declaration</li>
 * <li>only one {@link Event} type argument</li>
 * </ul>
 *
 * @see Event
 * @see EventListener
 * @since 2.7.5
 */
public abstract class GenericEventListener implements EventListener<Event> {

    private final Method onEventMethod;

    private final Map<Class<?>, Set<Method>> handleEventMethods;

    protected GenericEventListener() {
        this.onEventMethod = findOnEventMethod();
        this.handleEventMethods = findHandleEventMethods();
    }

    private Method findOnEventMethod() {
        return execute(getClass(), listenerClass -> listenerClass.getMethod("onEvent", Event.class));
    }

    private Map<Class<?>, Set<Method>> findHandleEventMethods() {
        // Event class for key, the eventMethods' Set as value
        Map<Class<?>, Set<Method>> eventMethods = new HashMap<>();
        of(getClass().getMethods())
                .filter(this::isHandleEventMethod)
                .forEach(method -> {
                    Class<?> paramType = method.getParameterTypes()[0];
                    Set<Method> methods = eventMethods.computeIfAbsent(paramType, key -> new LinkedHashSet<>());
                    methods.add(method);
                });
        return eventMethods;
    }

    public final void onEvent(Event event) {
        Class<?> eventClass = event.getClass();
        handleEventMethods.getOrDefault(eventClass, emptySet()).forEach(method -> {
            ThrowableConsumer.execute(method, m -> {
                m.invoke(this, event);
            });
        });
    }

    /**
     * The {@link Event event} handle methods must meet following conditions:
     * <ul>
     * <li>not {@link #onEvent(Event)} method</li>
     * <li><code>public</code> accessibility</li>
     * <li><code>void</code> return type</li>
     * <li>no {@link Exception exception} declaration</li>
     * <li>only one {@link Event} type argument</li>
     * </ul>
     *
     * @param method
     * @return
     */
    private boolean isHandleEventMethod(Method method) {

        if (onEventMethod.equals(method)) { // not {@link #onEvent(Event)} method
            return false;
        }

        if (!Modifier.isPublic(method.getModifiers())) { // not public
            return false;
        }

        if (!void.class.equals(method.getReturnType())) { // void return type
            return false;
        }

        Class[] exceptionTypes = method.getExceptionTypes();

        if (exceptionTypes.length > 0) { // no exception declaration
            return false;
        }

        Class[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) { // not only one argument
            return false;
        }

        if (!Event.class.isAssignableFrom(paramTypes[0])) { // not Event type argument
            return false;
        }

        return true;
    }
}
