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
package org.apache.dubbo.config.spring.context;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.model.ModuleModel;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;

import static org.apache.dubbo.common.extension.ExtensionScope.FRAMEWORK;

/**
 * Custom dubbo spring initialization
 */
@SPI(scope = FRAMEWORK)
public interface DubboSpringInitCustomizer {

    /**
     * <p>Customize dubbo spring initialization on bean registry processing phase.</p>
     * <p>You can register a {@link BeanFactoryPostProcessor} or {@link BeanPostProcessor} for custom processing.</p>
     * <p>Or change the bind module model via {@link DubboSpringInitContext#setModuleModel(ModuleModel)}.</p>
     *
     * <p><b>Note:</b></p>
     * <p>1. The bean factory may be not ready yet when triggered by parsing dubbo xml definition.</p>
     * <p>2. Some bean definitions may be not registered at this moment. If you plan to process all bean definitions,
     * it is recommended to register a custom {@link BeanFactoryPostProcessor} to do so.</p>
     *
     * @param context
     */
    void customize(DubboSpringInitContext context);

}
