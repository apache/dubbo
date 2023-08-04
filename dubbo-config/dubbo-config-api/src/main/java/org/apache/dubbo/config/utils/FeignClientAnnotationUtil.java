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

import org.apache.dubbo.common.utils.AnnotationUtils;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Map;

import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;

public class FeignClientAnnotationUtil {
    /**
     * append parameters from interface class metadata
     * such as FeignClient service as dubbo providedBy
     *
     * @param paramtetersMap
     */
    public static void appendParametersFromInterfaceClassMetadata(Class<?> interfaceClass, Map<String, String> paramtetersMap) {

        if (interfaceClass == null) {
            return;
        }

        Class<? extends Annotation> feignClientAnno = (Class<? extends Annotation>) ClassUtils.forNameAndTryCatch("org.springframework.cloud.openfeign.FeignClient");


        if (feignClientAnno == null || !AnnotationUtils.isAnnotationPresent(interfaceClass, feignClientAnno)) {
            return;
        }

        Annotation annotation = interfaceClass.getAnnotation(feignClientAnno);

        // get feign client service name
        String serviceName = AnnotationUtils.getAttribute(annotation, "name", "value");

        if (StringUtils.isEmpty(serviceName)) {
            return;
        }

        // append old value
        serviceName = paramtetersMap.containsKey(PROVIDED_BY) ? paramtetersMap.get(PROVIDED_BY) + "," + serviceName : serviceName;

        // cover old value
        paramtetersMap.put(PROVIDED_BY, serviceName);

    }
}
