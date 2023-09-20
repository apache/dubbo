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
package org.apache.dubbo.config.deploy.lifecycle.application;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.config.deploy.lifecycle.Lifecycle;
import org.apache.dubbo.rpc.model.ModuleModel;

/**
 * ApplicationLifecycle.
 * <br>
 * Used in an application Lifecycle managing procedure, and dubbo packages
 * can implement this interface to define what to do when application status changes.
 * <p>
 * Note: If you want to auto registry some extensions to BeanFactory that shared in an application, consider use {@link ApplicationDeployListener}.
 * <br>
 */
@SPI
public interface ApplicationLifecycle extends Lifecycle<ApplicationContext> {

    /**
     * @see org.apache.dubbo.config.deploy.lifecycle.manager.ApplicationLifecycleManager#preModuleChanged(ApplicationContext, ModuleModel, DeployState)
     */
    default void preModuleChanged(ApplicationContext applicationContext,ModuleModel changedModule,DeployState moduleState){}

    /**
     * @see org.apache.dubbo.config.deploy.lifecycle.manager.ApplicationLifecycleManager#postModuleChanged(ApplicationContext, ModuleModel, DeployState, DeployState, DeployState)
     */
    default void postModuleChanged(ApplicationContext applicationContext,ModuleModel changedModule,DeployState moduleNewState,DeployState applicationOldState,DeployState applicationNewState){}

    /**
     * @see org.apache.dubbo.config.deploy.lifecycle.manager.ApplicationLifecycleManager#runRefreshServiceInstance(ApplicationContext)
     */
    default void refreshServiceInstance(ApplicationContext applicationContext){}
}
