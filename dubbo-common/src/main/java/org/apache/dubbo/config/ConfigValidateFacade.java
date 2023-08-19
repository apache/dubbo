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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ConfigValidateFacade implements ConfigValidator {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(ConfigValidateFacade.class);

    private static final AtomicReference<ConfigValidateFacade> INSTANCE = new AtomicReference<>();

    private final List<ConfigValidator> validators;

    private ConfigValidateFacade(ScopeModel scopeModel) {
        ExtensionLoader<ConfigValidator> extensionLoader = scopeModel.getExtensionLoader(ConfigValidator.class);
        this.validators = extensionLoader.getActivateExtensions();
        this.validators.forEach(scopeModel.getBeanFactory()::registerBean);
    }

    public static ConfigValidateFacade getInstance() {
        INSTANCE.compareAndSet(null, new ConfigValidateFacade(FrameworkModel.defaultModel()));
        return INSTANCE.get();
    }

    @Override
    public void validate(AbstractConfig config) {
        if (config == null) {
            return;
        }
        boolean validated = false;
        for (ConfigValidator validator : validators) {
            if (validator.isSupport(config.getClass())) {
                validator.validate(config);
                validated = true;
                break;
            }
        }
        if (!validated) {
            LOGGER.warn("Config validate failed. No supported ConfigValidator found for config:" + config.getClass().getSimpleName());
        }
    }

    @Override
    public boolean isSupport(Class configClass) {
        return true;
    }

}
