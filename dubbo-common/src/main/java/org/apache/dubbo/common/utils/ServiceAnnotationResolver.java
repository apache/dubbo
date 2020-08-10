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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.config.annotation.Service;

import java.lang.annotation.Annotation;

import static java.lang.String.format;
import static org.apache.dubbo.common.utils.AnnotationUtils.getAttribute;
import static org.apache.dubbo.common.utils.ArrayUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.ClassUtils.isGenericClass;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;

/**
 * The resolver class for {@link Service @Service}
 *
 * @see Service
 * @see com.alibaba.dubbo.config.annotation.Service
 * @since 2.7.6
 */
public class ServiceAnnotationResolver {

    private final Annotation serviceAnnotation;

    private final Class<?> serviceType;

    public ServiceAnnotationResolver(Class<?> serviceType) throws IllegalArgumentException {
        this.serviceType = serviceType;
        this.serviceAnnotation = getServiceAnnotation(serviceType);
    }

    private Annotation getServiceAnnotation(Class<?> serviceType) {

        Annotation serviceAnnotation = serviceType.getAnnotation(Service.class);

        if (serviceAnnotation == null) {
            serviceAnnotation = serviceType.getAnnotation(com.alibaba.dubbo.config.annotation.Service.class);
        }

        if (serviceAnnotation == null) {
            throw new IllegalArgumentException(format("@%s or @%s can't be found in the service type[%s].",
                    Service.class.getName(),
                    com.alibaba.dubbo.config.annotation.Service.class.getName(),
                    serviceType.getName()
            ));
        }

        return serviceAnnotation;
    }

    /**
     * Resolve the class name of interface
     *
     * @return if not found, return <code>null</code>
     */
    public String resolveInterfaceClassName() {

        Class interfaceClass = null;
        // first, try to get the value from "interfaceName" attribute
        String interfaceName = resolveAttribute("interfaceName");

        if (isEmpty(interfaceName)) { // If not found, try "interfaceClass"
            interfaceClass = resolveAttribute("interfaceClass");
        } else {
            interfaceClass = resolveClass(interfaceName, getClass().getClassLoader());
        }

        if (isGenericClass(interfaceClass)) {
            interfaceName = interfaceClass.getName();
        } else {
            interfaceName = null;
        }

        if (isEmpty(interfaceName)) { // If not fund, try to get the first interface from the service type
            Class[] interfaces = serviceType.getInterfaces();
            if (isNotEmpty(interfaces)) {
                interfaceName = interfaces[0].getName();
            }
        }

        return interfaceName;
    }

    public String resolveVersion() {
        return resolveAttribute("version");
    }

    public String resolveGroup() {
        return resolveAttribute("group");
    }

    private <T> T resolveAttribute(String attributeName) {
        return getAttribute(serviceAnnotation, attributeName);
    }

    public Annotation getServiceAnnotation() {
        return serviceAnnotation;
    }

    public Class<?> getServiceType() {
        return serviceType;
    }
}
