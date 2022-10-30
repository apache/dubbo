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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.spring.ServiceBean;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *  {@link DubboService} Metadata
 *
 * @since 2.7.0
 */
@Component
public class DubboServicesMetadata extends AbstractDubboMetadata {

    public Map<String, Map<String, Object>> services() {

        Map<String, ServiceBean> serviceBeansMap = getServiceBeansMap();

        Map<String, Map<String, Object>> servicesMetadata = new LinkedHashMap<>(serviceBeansMap.size());

        for (Map.Entry<String, ServiceBean> entry : serviceBeansMap.entrySet()) {

            String serviceBeanName = entry.getKey();

            ServiceBean serviceBean = entry.getValue();

            Map<String, Object> serviceBeanMetadata = resolveBeanMetadata(serviceBean);

            Object service = resolveServiceBean(serviceBeanName, serviceBean);

            if (service != null) {
                // Add Service implementation class
                serviceBeanMetadata.put("serviceClass", service.getClass().getName());
            }

            servicesMetadata.put(serviceBeanName, serviceBeanMetadata);

        }

        return servicesMetadata;

    }

    private Object resolveServiceBean(String serviceBeanName, ServiceBean serviceBean) {

        int index = serviceBeanName.indexOf("#");

        if (index > -1) {

            Class<?> interfaceClass = serviceBean.getInterfaceClass();

            String serviceName = serviceBeanName.substring(index + 1);

            if (applicationContext.containsBean(serviceName)) {
                return applicationContext.getBean(serviceName, interfaceClass);
            }

        }

        return null;

    }

}
