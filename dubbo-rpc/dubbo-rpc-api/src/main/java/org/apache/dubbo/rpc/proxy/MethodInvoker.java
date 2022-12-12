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
package org.apache.dubbo.rpc.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MethodInvoker {

    Object invoke(Object instance, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable;

    /**
     * no overload method invoker
     */
    class SingleMethodInvoker implements MethodInvoker {

        private final Method method;

        SingleMethodInvoker(Method method) {
            this.method = method;
        }

        @Override
        public Object invoke(Object instance, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable {
            return method.invoke(instance, arguments);
        }
    }

    /**
     * overload method invoker
     */
    class OverloadMethodInvoker implements MethodInvoker {

        private final MethodMeta[] methods;

        OverloadMethodInvoker(List<Method> methods) {
            this.methods = methods.stream().map(MethodMeta::new).toArray(MethodMeta[]::new);
        }

        @Override
        public Object invoke(Object instance, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable {
            for (MethodMeta meta : methods) {
                if (Arrays.equals(meta.getParametersType(), parameterTypes)) {
                    return meta.getMethod().invoke(instance, arguments);
                }
            }
            throw new NoSuchMethodException(instance.getClass().getName() + "." + methodName + Arrays.toString(parameterTypes));
        }

        private static class MethodMeta {

            private final Method method;

            private final Class<?>[] parametersType;

            private MethodMeta(Method method) {
                this.method = method;
                this.parametersType = method.getParameterTypes();
            }

            public Method getMethod() {
                return method;
            }

            public Class<?>[] getParametersType() {
                return parametersType;
            }
        }
    }

    class CompositeMethodInvoker implements MethodInvoker {

        private final Map<String, MethodInvoker> invokers;

        public CompositeMethodInvoker(Map<String, MethodInvoker> invokers) {
            this.invokers = invokers;
        }

        /**
         * for test
         *
         * @return all MethodInvoker
         */
        Map<String, MethodInvoker> getInvokers() {
            return invokers;
        }

        @Override
        public Object invoke(Object instance, String methodName, Class<?>[] parameterTypes, Object[] arguments) throws Throwable {
            MethodInvoker invoker = invokers.get(methodName);
            if (invoker == null) {
                throw new NoSuchMethodException(instance.getClass().getName() + "." + methodName + Arrays.toString(parameterTypes));
            }
            return invoker.invoke(instance, methodName, parameterTypes, arguments);
        }
    }

    static MethodInvoker newInstance(Class<?> clazz) {
        Method[] methods = clazz.getMethods();
        Map<String, MethodInvoker> invokers = new HashMap<>();
        Map<String, List<Method>> map = new HashMap<>();
        for (Method method : methods) {
            map.computeIfAbsent(method.getName(), name -> new ArrayList<>()).add(method);
        }
        Set<Map.Entry<String, List<Method>>> entries = map.entrySet();
        for (Map.Entry<String, List<Method>> entry : entries) {
            String methodName = entry.getKey();
            List<Method> ms = entry.getValue();
            if (ms.size() == 1) {
                invokers.put(methodName, new SingleMethodInvoker(ms.get(0)));
                continue;
            }
            invokers.put(methodName, new OverloadMethodInvoker(ms));
        }
        return new CompositeMethodInvoker(invokers);
    }
}
