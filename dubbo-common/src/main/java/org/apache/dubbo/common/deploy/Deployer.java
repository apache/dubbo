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
package org.apache.dubbo.common.deploy;

import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.concurrent.Future;

public interface Deployer<E extends ScopeModel> {

    /**
     * Initialize the component
     */
    void initialize() throws IllegalStateException;

    /**
     * Starts the component.
     * @return
     */
    Future start() throws IllegalStateException;

    /**
     * Stops the component.
     */
    void stop() throws IllegalStateException;

    /**
     * @return true if the component is added and waiting to start
     */
    boolean isPending();

    /**
     * @return true if the component is starting or has been started.
     */
    boolean isRunning();

    /**
     * @return true if the component has been started.
     * @see #start()
     * @see #isStarting()
     */
    boolean isStarted();

    /**
     * @return true if the component is starting.
     * @see #isStarted()
     */
    boolean isStarting();

    /**
     * @return true if the component is stopping.
     * @see #isStopped()
     */
    boolean isStopping();

    /**
     * @return true if the component is stopping.
     * @see #isStopped()
     */
    boolean isStopped();

    /**
     * @return true if the component has failed to start or has failed to stop.
     */
    boolean isFailed();

    /**
     * @return current state
     */
    DeployState getState();

    void addDeployListener(DeployListener<E> listener);

    void removeDeployListener(DeployListener<E> listener);

    Throwable getError();
}
