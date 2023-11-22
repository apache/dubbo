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

public interface DeployListener<E extends ScopeModel> {
    /**
     * Useful to inject some configuration like MetricsConfig, RegistryConfig, etc.
     */
    void onInitialize(E scopeModel);

    /**
     * Triggered before starting module.
     */
    void onStarting(E scopeModel);

    /**
     * Triggered before registering and exposing the service.
     */
    void onStarted(E scopeModel);

    /**
     * Triggered after deployer startup is complete.
     */
    void onCompletion(E scopeModel);

    /**
     * Triggered before the app is destroyed,
     * can do some customized things before offline the service and destroy reference.
     */
    void onStopping(E scopeModel);

    /**
     * Triggered after the application is destroyed,
     * can do some customized things after the service is offline and the reference is destroyed.
     */
    void onStopped(E scopeModel);

    /**
     * Useful to do something when deployer was failed.
     */
    void onFailure(E scopeModel, Throwable cause);
}
