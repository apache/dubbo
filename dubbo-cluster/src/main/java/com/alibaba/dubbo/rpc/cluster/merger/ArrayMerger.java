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
package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

import java.lang.reflect.Array;

/**
 *
 */
public class ArrayMerger implements Merger<Object[]> {

    public static final ArrayMerger INSTANCE = new ArrayMerger();

    public Object[] merge(Object[]... others) {
        if (others.length == 0) {
            return null;
        }
        int totalLen = 0;
        for (int i = 0; i < others.length; i++) {
            Object item = others[i];
            if (item != null && item.getClass().isArray()) {
                totalLen += Array.getLength(item);
            } else {
                throw new IllegalArgumentException(
                        new StringBuilder(32).append(i + 1)
                                .append("th argument is not an array").toString());
            }
        }

        if (totalLen == 0) {
            return null;
        }

        Class<?> type = others[0].getClass().getComponentType();

        Object result = Array.newInstance(type, totalLen);
        int index = 0;
        for (Object array : others) {
            for (int i = 0; i < Array.getLength(array); i++) {
                Array.set(result, index++, Array.get(array, i));
            }
        }
        return (Object[]) result;
    }

}
