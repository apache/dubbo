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
package org.apache.dubbo.config.spring.context.properties;

import org.apache.dubbo.config.AbstractConfig;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.alibaba.spring.util.PropertySourcesUtils.getSubProperties;

/**
 * Default {@link DubboConfigBinder} implementation based on Spring {@link DataBinder}
 */
public class DefaultDubboConfigBinder extends AbstractDubboConfigBinder {

    @Override
    public <C extends AbstractConfig> void bind(String prefix, C dubboConfig) {
        DataBinder dataBinder = new DataBinder(dubboConfig);
        // Set ignored*
        dataBinder.setIgnoreInvalidFields(isIgnoreInvalidFields());
        dataBinder.setIgnoreUnknownFields(isIgnoreUnknownFields());
        // Get properties under specified prefix from PropertySources
        Map<String, Object> properties = getSubProperties(getPropertySources(), prefix);
        // Convert Map to MutablePropertyValues
        MutablePropertyValues propertyValues = new MutablePropertyValues(properties);
        // Bind
        dataBinder.bind(propertyValues);
        BindingResult bindingResult = dataBinder.getBindingResult();
        if (bindingResult.hasGlobalErrors()) {
            throw new RuntimeException("Data bind global error, please check config. config: " + bindingResult.getGlobalError() + "");
        }
        if (bindingResult.hasFieldErrors()) {
            throw new RuntimeException(buildErrorMsg(bindingResult.getFieldErrors(), prefix, dubboConfig.getClass().getSimpleName()));
        }
    }

    private String buildErrorMsg(List<FieldError> errors, String prefix, String config) {
        StringBuilder builder = new StringBuilder("Data bind error, please check config. config: " + config + ", prefix: " + prefix
                + " , error fields: [" + errors.get(0).getField());
        if (errors.size() > 1) {
            IntStream.range(1, errors.size()).forEach(i -> {
                builder.append(", " + errors.get(i).getField());
            });
        }
        builder.append(']');
        return builder.toString();
    }
}

