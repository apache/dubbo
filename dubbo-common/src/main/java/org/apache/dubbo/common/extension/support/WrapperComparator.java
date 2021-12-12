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
package org.apache.dubbo.common.extension.support;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.Wrapper;

import java.util.Comparator;

/**
 * OrderComparator  
 * Derived from {@link ActivateComparator}
 */
public class WrapperComparator implements Comparator<Object> {

    public static final Comparator<Object> COMPARATOR = new WrapperComparator();

    @Override
    public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        if (o1.equals(o2)) {
            return 0;
        }

        Class clazz1 = (Class) o1;
        Class clazz2 = (Class) o2;
        
        OrderInfo a1 = parseOrder(clazz1);
        OrderInfo a2 = parseOrder(clazz2);

        int n1 = a1.order;
        int n2 = a2.order;
        // never return 0 even if n1 equals n2, otherwise, o1 and o2 will override each other in collection like HashSet
        return n1 > n2 ? 1 : -1;
    }

    private OrderInfo parseOrder(Class<?> clazz) {
        OrderInfo info = new OrderInfo();
        if (clazz.isAnnotationPresent(Activate.class)) {
            // TODO: backward compatibility
            Activate activate = clazz.getAnnotation(Activate.class);
            info.order = activate.order();
        } else if (clazz.isAnnotationPresent(com.alibaba.dubbo.common.extension.Activate.class)) {
            // TODO: backward compatibility
            com.alibaba.dubbo.common.extension.Activate activate = clazz.getAnnotation(
                    com.alibaba.dubbo.common.extension.Activate.class);
            info.order = activate.order();
        } else if (clazz.isAnnotationPresent(Wrapper.class)) {
            Wrapper wrapper = clazz.getAnnotation(Wrapper.class);
            info.order = wrapper.order();
        }
        return info;
    }

    private static class OrderInfo {
        private int order;
    }
}
