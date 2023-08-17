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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"rawtypes","unchecked"})
public class ConfigValidateFacade implements ConfigValidator {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ConfigValidateFacade.class);

    private static AtomicReference<ConfigValidateFacade> instance = new AtomicReference<>();

    private final List<ConfigValidator> validators;

    public ConfigValidateFacade(ScopeModel scopeModel) {
            ExtensionLoader<ConfigValidator> extensionLoader = scopeModel.getExtensionLoader(ConfigValidator.class);
            this.validators = extensionLoader.getActivateExtensions();
            this.validators.forEach(scopeModel.getBeanFactory()::registerBean);
            instance.set(this);
    }

    @Override
    public void validate(AbstractConfig config) {
        if(config == null){
            return;
        }
        AtomicBoolean validated = new AtomicBoolean(false);
        validators.forEach(
            configValidator ->  {
                if(configValidator.isSupport(config.getClass())){
                    configValidator.validate(config);
                    validated.set(true);
                }
        });
        if(!validated.get()){
           logger.warn("No supported ConfigValidator found for config:"+config.getClass().getSimpleName());
        }
    }

    @Override
    public boolean isSupport(Class configClass) {
        return true;
    }

    public static ConfigValidateFacade getInstance(){
        return instance.get();
    }

}
