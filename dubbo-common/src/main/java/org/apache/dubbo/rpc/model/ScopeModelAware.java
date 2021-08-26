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
package org.apache.dubbo.rpc.model;

/**
 * An interface to inject FrameworkModel/ApplicationModel/ModuleModel for SPI extensions and internal beans.
 */
public interface ScopeModelAware {

    /**
     * Override this method if you need get the scope model (maybe one of FrameworkModel/ApplicationModel/ModuleModel).
     * @param scopeModel
     */
    default void setScopeModel(ScopeModel scopeModel) {
    }

    /**
     * Override this method if you just need framework model
     * @param frameworkModel
     */
    default void setFrameworkModel(FrameworkModel frameworkModel) {
    }

    /**
     * Override this method if you just need application model
     * @param applicationModel
     */
    default void setApplicationModel(ApplicationModel applicationModel) {
    }

    /**
     * Override this method if you just need module model
     * @param moduleModel
     */
    default void setModuleModel(ModuleModel moduleModel) {
    }

}
