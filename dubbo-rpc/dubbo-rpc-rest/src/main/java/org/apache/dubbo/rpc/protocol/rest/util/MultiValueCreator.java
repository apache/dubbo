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
package org.apache.dubbo.rpc.protocol.rest.util;


import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;


public class MultiValueCreator {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MultiValueCreator.class);

    private final static String SPRING_MultiValueMapImpl = "org.springframework.util.LinkedMultiValueMap";
    private final static String SPRING_MultiValueMap = "org.springframework.util.MultiValueMap";
    private final static String JAVAX_MultiValueMapImpl = "org.jboss.resteasy.specimpl.MultivaluedMapImpl";
    private final static String JAVAX_MultiValueMap = "javax.ws.rs.core.MultivaluedMap";

    private static Class springMultiValueMapImplClass = null;
    private static Class springMultiValueMapClass = null;
    private static Method springMultiValueMapAdd = null;

    private static Class jaxrsMultiValueMapImplClass = null;
    private static Class jaxrsMultiValueMapClass = null;

    private static Method jaxrsMultiValueMapAdd = null;

    static {
        springMultiValueMapClass = ReflectUtils.findClassTryException(SPRING_MultiValueMap);
        springMultiValueMapImplClass = ReflectUtils.findClassTryException(SPRING_MultiValueMapImpl);
        springMultiValueMapAdd = ReflectUtils.getMethodByName(springMultiValueMapImplClass, "add");

        jaxrsMultiValueMapClass = ReflectUtils.findClassTryException(JAVAX_MultiValueMap);
        jaxrsMultiValueMapImplClass = ReflectUtils.findClassTryException(JAVAX_MultiValueMapImpl);
        jaxrsMultiValueMapAdd = ReflectUtils.getMethodByName(jaxrsMultiValueMapImplClass, "add");

    }


    public static Object providerCreateMultiValueMap(Class<?> targetType) {
        try {
            if (typeJudge(springMultiValueMapClass, targetType)) {
                return springMultiValueMapImplClass.getDeclaredConstructor().newInstance();
            } else if (typeJudge(jaxrsMultiValueMapClass, targetType)) {
                return jaxrsMultiValueMapImplClass.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            logger.error("", e.getMessage(), "current param type is: " + targetType + "and support type is : " + springMultiValueMapClass + "or" + jaxrsMultiValueMapClass,
                "dubbo rest form content-type param construct error,un support  param type: ", e);
        }

        return null;
    }


    private static boolean typeJudge(Class<?> parent, Class<?> targetType) {
        if (parent == null) {
            return false;
        }

        if (!Map.class.isAssignableFrom(targetType)) {
            return true;
        }

        return parent.isAssignableFrom(targetType) || parent.equals(targetType);
    }

    public static void add(Object multiValueMap, String key, Object value) {
        try {
            if (multiValueMap == null) {
                return;
            }

            Method multiValueMapAdd = null;
            if (springMultiValueMapImplClass.equals(multiValueMap.getClass())) {
                multiValueMapAdd = springMultiValueMapAdd;
            } else if (jaxrsMultiValueMapImplClass.equals(multiValueMap.getClass())) {
                multiValueMapAdd = jaxrsMultiValueMapAdd;
            }

            ReflectUtils.invokeAndTryCatch(multiValueMap, multiValueMapAdd, new Object[]{key, value});
        } catch (Exception e) {
            logger.error("", e.getMessage(), "", "dubbo rest form content-type param add data  error: ", e);
        }
    }


}
