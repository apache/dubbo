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
package org.apache.dubbo.spring.boot.autoconfigure;

import org.apache.dubbo.config.spring.context.properties.DubboConfigBinder;

import com.alibaba.spring.context.config.ConfigurationBeanBinder;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;

import java.util.Map;


/**
 * Spring Boot Relaxed {@link DubboConfigBinder} implementation
 *
 * @since 2.7.0
 */
class RelaxedDubboConfigBinder implements ConfigurationBeanBinder {

    @Override
    public void bind(Map<String, Object> configurationProperties, boolean ignoreUnknownFields,
                     boolean ignoreInvalidFields, Object configurationBean) {
        RelaxedDataBinder relaxedDataBinder = new RelaxedDataBinder(configurationBean);
        // Set ignored*
        relaxedDataBinder.setIgnoreInvalidFields(ignoreInvalidFields);
        relaxedDataBinder.setIgnoreUnknownFields(ignoreUnknownFields);
        // Get properties under specified prefix from PropertySources
        // Convert Map to MutablePropertyValues
        MutablePropertyValues propertyValues = new MutablePropertyValues(configurationProperties);
        // Bind
        relaxedDataBinder.bind(propertyValues);
    }
}
