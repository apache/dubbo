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
package org.apache.dubbo.common.context;

/**
 * The Lifecycle of Dubbo component
 *
 * @since 2.7.4
 */
public interface Lifecycle {

    /**
     * Initialize the component before {@link #start() start}
     *
     * @return current {@link Lifecycle}
     * @throws IllegalStateException
     */
    Lifecycle initialize() throws IllegalStateException;

    /**
     * Initialized or not
     *
     * @return if initialized, return <code>true</code>, or <code>false</code>
     */
    boolean isInitialized();

    /**
     * Start the component
     *
     * @return current {@link Lifecycle}
     * @throws IllegalStateException
     */
    Lifecycle start() throws IllegalStateException;

    /**
     * The component is started or not
     *
     * @return if started, return <code>true</code>, or <code>false</code>
     */
    boolean isStarted();

    /**
     * Stop the component
     *
     * @return current {@link Lifecycle}
     */
    Lifecycle stop() throws IllegalStateException;

    /**
     * The component is stopped or not
     *
     * @return if stopped, return <code>true</code>, or <code>false</code>
     */
    default boolean isStopped() {
        return !isStarted();
    }

    /**
     * Destroy the component
     *
     * @throws IllegalStateException
     */
    void destroy() throws IllegalStateException;
}
