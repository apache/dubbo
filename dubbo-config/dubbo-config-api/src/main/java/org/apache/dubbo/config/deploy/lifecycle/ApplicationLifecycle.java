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
package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.deploy.lifecycle.context.ApplicationContext;
import org.apache.dubbo.config.deploy.lifecycle.event.AppInitEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPostDestroyEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPostModuleChangeEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPreDestroyEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppPreModuleChangeEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppServiceRefreshEvent;
import org.apache.dubbo.config.deploy.lifecycle.event.AppStartEvent;
import org.apache.dubbo.rpc.model.ModuleModel;

/**
 * ApplicationLifecycle.
 * <br>
 * Used in an application Lifecycle managing procedure, and dubbo packages
 * can implement this interface to define what to do when application status changes.
 * <br>
 * In another word, when methods like
 * {@link ApplicationContext#runInitialize()},
 * {@link ApplicationContext#runStart()},
 * {@link ApplicationContext#runPreDestroy()},
 * {@link ApplicationContext#runPostDestroy()} etc.
 * called, all implementations of this interface will also be called.
 */
@SPI
public interface ApplicationLifecycle extends Lifecycle {

    /**
     * @see ApplicationContext#runStart()
     */
    default void start(AppStartEvent startContext){}

    /**
     * @see ApplicationContext#runInitialize()
     */
    default void initialize(AppInitEvent initContext){};

    /**
     * @see ApplicationContext#runPreDestroy()
     */
    default void preDestroy(AppPreDestroyEvent preDestroyContext) {}

    /**
     * @see ApplicationContext#runPostDestroy()
     */
    default void postDestroy(AppPostDestroyEvent postDestroyContext) {}

    /**
     * @see ApplicationContext#runPreModuleChanged(ModuleModel, DeployState)
     */
    default void preModuleChanged(AppPreModuleChangeEvent preModuleChangeContext){}

    /**
     * @see ApplicationContext#runPostModuleChanged(ModuleModel, DeployState, DeployState, DeployState)
     */
    default void postModuleChanged(AppPostModuleChangeEvent postModuleChangeContext){}

    /**
     * @see ApplicationContext#runRefreshServiceInstance()
     */
    default void refreshServiceInstance(AppServiceRefreshEvent serviceRefreshContext){}

}
