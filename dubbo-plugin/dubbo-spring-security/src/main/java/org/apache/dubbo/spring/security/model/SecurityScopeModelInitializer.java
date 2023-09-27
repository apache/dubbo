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

package org.apache.dubbo.spring.security.model;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;
import org.apache.dubbo.spring.security.jackson.ObjectMapperCodec;
import org.apache.dubbo.spring.security.jackson.ObjectMapperCodecCustomer;

import java.util.Set;

import static org.apache.dubbo.spring.security.utils.SecurityNames.CORE_JACKSON_2_MODULE_CLASS_NAME;
import static org.apache.dubbo.spring.security.utils.SecurityNames.JAVA_TIME_MODULE_CLASS_NAME;
import static org.apache.dubbo.spring.security.utils.SecurityNames.OBJECT_MAPPER_CLASS_NAME;
import static org.apache.dubbo.spring.security.utils.SecurityNames.SECURITY_CONTEXT_HOLDER_CLASS_NAME;
import static org.apache.dubbo.spring.security.utils.SecurityNames.SIMPLE_MODULE_CLASS_NAME;

@Activate(onClass = {SECURITY_CONTEXT_HOLDER_CLASS_NAME, CORE_JACKSON_2_MODULE_CLASS_NAME, OBJECT_MAPPER_CLASS_NAME,
    JAVA_TIME_MODULE_CLASS_NAME, SIMPLE_MODULE_CLASS_NAME})
public class SecurityScopeModelInitializer implements ScopeModelInitializer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {
        ScopeBeanFactory beanFactory = frameworkModel.getBeanFactory();

        try {
            ObjectMapperCodec objectMapperCodec = new ObjectMapperCodec();

            Set<ObjectMapperCodecCustomer> objectMapperCodecCustomerList = frameworkModel.getExtensionLoader(ObjectMapperCodecCustomer.class).getSupportedExtensionInstances();

            for (ObjectMapperCodecCustomer objectMapperCodecCustomer : objectMapperCodecCustomerList) {
                objectMapperCodecCustomer.customize(objectMapperCodec);
            }

            beanFactory.registerBean(objectMapperCodec);
        } catch (Throwable t) {
            logger.info("Failed to initialize ObjectMapperCodecCustomer and spring security related features are disabled.", t);
        }
    }

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {
    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {


    }

}
