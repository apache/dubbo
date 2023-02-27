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


import java.lang.reflect.Method;


public class MultiValueCreator {
    private final static String SPRING_MultiValueMap = "org.springframework.util.LinkedMultiValueMap";
    private final static String JAVAX_MultiValueMap = "org.jboss.resteasy.specimpl.MultivaluedMapImpl";

    private static Class multiValueMapClass = null;
    private static Method multiValueMapAdd = null;

    static {
        multiValueMapClass = ReflectUtils.findClassTryException(SPRING_MultiValueMap, JAVAX_MultiValueMap);
        multiValueMapAdd = ReflectUtils.getMethodAndTryCatch(multiValueMapClass, "add", new Class[]{Object.class, Object.class});
    }


    public static Object createMultiValueMap() {
        try {
            return multiValueMapClass.newInstance();
        } catch (Exception e) {

        }

        return null;
    }

    public static void add(Object multiValueMap, String key, String value) {
        try {
            ReflectUtils.invokeAndTryCatch(multiValueMap, multiValueMapAdd, new String[]{key, value});
        } catch (Exception e) {

        }
    }


}
