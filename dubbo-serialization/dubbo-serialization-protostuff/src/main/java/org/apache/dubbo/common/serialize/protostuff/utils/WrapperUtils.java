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

package org.apache.dubbo.common.serialize.protostuff.utils;

import org.apache.dubbo.common.serialize.protostuff.Wrapper;
import org.apache.dubbo.common.serialize.protostuff.delegate.TimeDelegate;

import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.RuntimeEnv;
import org.apache.dubbo.common.serialize.protostuff.delegate.TimestampDelegate;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use WrapperUtils to wrap object to {@link Wrapper}
 */
public class WrapperUtils {
    private static final Set<Class<?>> WRAPPER_SET = new HashSet<>();

    static {
        if (RuntimeEnv.ID_STRATEGY instanceof DefaultIdStrategy) {
            ((DefaultIdStrategy) RuntimeEnv.ID_STRATEGY).registerDelegate(new TimeDelegate());
            ((DefaultIdStrategy) RuntimeEnv.ID_STRATEGY).registerDelegate(new TimestampDelegate());
        }

        WRAPPER_SET.add(Map.class);
        WRAPPER_SET.add(HashMap.class);
        WRAPPER_SET.add(TreeMap.class);
        WRAPPER_SET.add(Hashtable.class);
        WRAPPER_SET.add(SortedMap.class);
        WRAPPER_SET.add(LinkedHashMap.class);
        WRAPPER_SET.add(ConcurrentHashMap.class);

        WRAPPER_SET.add(List.class);
        WRAPPER_SET.add(ArrayList.class);
        WRAPPER_SET.add(LinkedList.class);

        WRAPPER_SET.add(Vector.class);

        WRAPPER_SET.add(Set.class);
        WRAPPER_SET.add(HashSet.class);
        WRAPPER_SET.add(TreeSet.class);
        WRAPPER_SET.add(BitSet.class);

        WRAPPER_SET.add(StringBuffer.class);
        WRAPPER_SET.add(StringBuilder.class);

        WRAPPER_SET.add(BigDecimal.class);
        WRAPPER_SET.add(Date.class);
        WRAPPER_SET.add(Calendar.class);
        WRAPPER_SET.add(Time.class);
        WRAPPER_SET.add(Timestamp.class);

        WRAPPER_SET.add(Wrapper.class);

    }

    /**
     * Determine if the object needs wrap
     *
     * @param clazz object type
     * @return need wrap
     */
    public static boolean needWrapper(Class<?> clazz) {
        return WrapperUtils.WRAPPER_SET.contains(clazz) || clazz.isArray() || clazz.isEnum();
    }

    /**
     * Determine if the object needs wrap
     *
     * @param obj object
     * @return need wrap
     */
    public static boolean needWrapper(Object obj) {
        return needWrapper(obj.getClass());
    }

}
