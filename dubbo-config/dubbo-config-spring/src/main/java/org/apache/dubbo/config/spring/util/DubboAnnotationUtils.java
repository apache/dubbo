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
package org.apache.dubbo.config.spring.util;

import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.rpc.service.GenericService;

import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.alibaba.spring.util.AnnotationUtils.getAttribute;
import static org.springframework.util.ClassUtils.getAllInterfacesForClass;
import static org.springframework.util.StringUtils.hasText;

/**
 * Dubbbo Annotation Utilities Class
 *
 * @see org.springframework.core.annotation.AnnotationUtils
 * @since 2.5.11
 */
public class DubboAnnotationUtils {


    @Deprecated
    public static String resolveInterfaceName(Service service, Class<?> defaultInterfaceClass)
            throws IllegalStateException {

        String interfaceName;
        if (hasText(service.interfaceName())) {
            interfaceName = service.interfaceName();
        } else if (!void.class.equals(service.interfaceClass())) {
            interfaceName = service.interfaceClass().getName();
        } else if (defaultInterfaceClass.isInterface()) {
            interfaceName = defaultInterfaceClass.getName();
        } else {
            throw new IllegalStateException(
                    "The @Service undefined interfaceClass or interfaceName, and the type "
                            + defaultInterfaceClass.getName() + " is not a interface.");
        }

        return interfaceName;

    }

    /**
     * Resolve the service interface name from @Service annotation attributes.
     * <p/>
     * Note: the service interface class maybe not found locally if is a generic service.
     *
     * @param attributes             annotation attributes of {@link Service @Service}
     * @param defaultInterfaceClass the default class of interface
     * @return the interface name if found
     * @throws IllegalStateException if interface name was not found
     */
    public static String resolveInterfaceName(Map<String, Object> attributes, Class<?> defaultInterfaceClass) {
        Boolean generic = getAttribute(attributes, "generic");
        // 1. get from DubboService.interfaceName()
        String interfaceClassName = getAttribute(attributes, "interfaceName");
        if (StringUtils.hasText(interfaceClassName)) {
            if (GenericService.class.getName().equals(interfaceClassName) ||
                com.alibaba.dubbo.rpc.service.GenericService.class.getName().equals(interfaceClassName)) {
                throw new IllegalStateException("@Service interfaceName() cannot be GenericService: " + interfaceClassName);
            }
            return interfaceClassName;
        }

        // 2. get from DubboService.interfaceClass()
        Class<?> interfaceClass = getAttribute(attributes, "interfaceClass");
        if (interfaceClass == null || void.class.equals(interfaceClass)) { // default or set void.class for purpose.
            interfaceClass = null;
        } else  if (GenericService.class.isAssignableFrom(interfaceClass)) {
            throw new IllegalStateException("@Service interfaceClass() cannot be GenericService :" + interfaceClass.getName());
        }

        // 3. get from annotation element type, ignore GenericService
        if (interfaceClass == null && defaultInterfaceClass != null  && !GenericService.class.isAssignableFrom(defaultInterfaceClass)) {
            // Find all interfaces from the annotated class
            // To resolve an issue : https://github.com/apache/dubbo/issues/3251
            Class<?>[] allInterfaces = getAllInterfacesForClass(defaultInterfaceClass);
            if (allInterfaces.length > 0) {
                interfaceClass = allInterfaces[0];
            }
        }

        Assert.notNull(interfaceClass, "@Service interfaceClass() or interfaceName() or interface class must be present!");
        Assert.isTrue(interfaceClass.isInterface(), "The annotated type must be an interface!");
        return interfaceClass.getName();
    }

    @Deprecated
    public static String resolveInterfaceName(Reference reference, Class<?> defaultInterfaceClass)
            throws IllegalStateException {

        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!void.class.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else if (defaultInterfaceClass.isInterface()) {
            interfaceName = defaultInterfaceClass.getName();
        } else {
            throw new IllegalStateException(
                    "The @Reference undefined interfaceClass or interfaceName, and the type "
                            + defaultInterfaceClass.getName() + " is not a interface.");
        }

        return interfaceName;
    }

    /**
     * Resolve the parameters of {@link org.apache.dubbo.config.annotation.DubboService}
     * and {@link org.apache.dubbo.config.annotation.DubboReference} from the specified.
     * It iterate elements in order.The former element plays as key or key&value role, it would be
     * spilt if it contain specific string, for instance, ":" and "=". As for later element can't
     * be split in anytime.It will throw IllegalArgumentException If converted array length isn't
     * even number.
     * The convert cases below work in right way,which are best practice.
     * <p>
     * (array->map)
     * ["a","b"] ==> {a=b}
     * [" a "," b "] ==> {a=b}
     * ["a=b"] ==>{a=b}
     * ["a:b"] ==>{a=b}
     * ["a=b","c","d"] ==>{a=b,c=d}
     * ["a","a:b"] ==>{a="a:b"}
     * ["a","a,b"] ==>{a="a,b"}
     * </p>
     *
     * @param parameters
     * @return
     */
    public static Map<String, String> convertParameters(String[] parameters) {
        if (ArrayUtils.isEmpty(parameters)) {
            return null;
        }

        List<String> compatibleParameterArray = Arrays.stream(parameters)
            .map(String::trim)
            .reduce(new ArrayList<>(parameters.length), (list, parameter) ->
                {
                    if (list.size() % 2 == 1) {
                        //value doesn't split
                        list.add(parameter);
                        return list;
                    }

                    String[] sp1 = parameter.split(":");
                    if (sp1.length > 0 && sp1.length % 2 == 0) {
                        //key split
                        list.addAll(Arrays.stream(sp1).map(String::trim).collect(Collectors.toList()));
                        return list;
                    }
                    sp1 = parameter.split("=");
                    if (sp1.length > 0 && sp1.length % 2 == 0) {
                        list.addAll(Arrays.stream(sp1).map(String::trim).collect(Collectors.toList()));
                        return list;
                    }
                    list.add(parameter);
                    return list;
                }
                , (a, b) -> a);

        return CollectionUtils.toStringMap(compatibleParameterArray.toArray(new String[0]));
    }
}
