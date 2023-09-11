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
package org.apache.dubbo.config;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.validator.ConsumerConfigValidator;
import org.apache.dubbo.config.validator.ProviderConfigValidator;
import org.apache.dubbo.config.validator.ReferenceConfigValidator;
import org.apache.dubbo.config.validator.ServiceConfigValidator;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;


public class ConfigValidatorExistTest {

    ConfigValidateFacade validateFacade;

    @Test
    void testConfigValidatorExist(){
        FrameworkModel frameworkModel = FrameworkModel.defaultModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();

        validateFacade = new ConfigValidateFacade(applicationModel);
        Assertions.assertNotNull(validateFacade);
        Assertions.assertTrue(validateFacade.getValidators().size() >= 4);

        try(
                MockedStatic<ConsumerConfigValidator> configValidatorMockedStatic = Mockito.mockStatic(ConsumerConfigValidator.class);
                MockedStatic<ProviderConfigValidator> providerConfigValidatorMockedStatic = Mockito.mockStatic(ProviderConfigValidator.class);
                MockedStatic<ReferenceConfigValidator> referenceConfigValidatorMockedStatic = Mockito.mockStatic(ReferenceConfigValidator.class);
                MockedStatic<ServiceConfigValidator> serviceConfigValidatorMockedStatic = Mockito.mockStatic(ServiceConfigValidator.class);
        ){
            configValidatorMockedStatic.when(()-> ConsumerConfigValidator.validateConsumerConfig(any())).thenCallRealMethod();
            providerConfigValidatorMockedStatic.when(()-> ProviderConfigValidator.validateProviderConfig(any())).thenCallRealMethod();
            referenceConfigValidatorMockedStatic.when(()-> ReferenceConfigValidator.validateReferenceConfig(any())).thenCallRealMethod();
            serviceConfigValidatorMockedStatic.when(()->ServiceConfigValidator.validateServiceConfig(any())).thenCallRealMethod();
            triggerValidate(new ConsumerConfig());
            triggerValidate(new AbstractInterfaceConfig() {
                @Override
                public List<URL> getExportedUrls() {
                    return super.getExportedUrls();
                }
            });
            triggerValidate(new MethodConfig());
            triggerValidate(new ProviderConfig());
            triggerValidate(new ReferenceConfig<>());
            triggerValidate(new ServiceConfig<>());

            configValidatorMockedStatic.verify(()-> ConsumerConfigValidator.validateConsumerConfig(any()),atLeastOnce());
            providerConfigValidatorMockedStatic.verify(()->ProviderConfigValidator.validateProviderConfig(any()),atLeastOnce());
            referenceConfigValidatorMockedStatic.verify(()->ReferenceConfigValidator.validateReferenceConfig(any()),atLeastOnce());
            serviceConfigValidatorMockedStatic.verify(()-> ServiceConfigValidator.validateServiceConfig(any()),atLeastOnce());
        }
    }

    void triggerValidate(AbstractConfig config){
        try {
            validateFacade.validate(config);
        }catch (Throwable ignored){};
    }

}
