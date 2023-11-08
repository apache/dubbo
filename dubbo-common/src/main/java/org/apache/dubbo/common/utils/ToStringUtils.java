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

import org.apache.dubbo.config.AbstractConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ToStringUtils {

    private ToStringUtils() {}

    public static String printToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JsonUtils.toJson(obj);
        } catch (Throwable throwable) {
            if (obj instanceof Object[]) {
                return Arrays.toString((Object[]) obj);
            }
            return obj.toString();
        }
    }

    public static String toString(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (ClassUtils.isSimpleType(obj.getClass())) {
            return obj.toString();
        }
        if (obj.getClass().isPrimitive()) {
            return obj.toString();
        }
        if (obj instanceof Object[]) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            Object[] objects = (Object[]) obj;
            for (int i = 0; i < objects.length; i++) {
                stringBuilder.append(toString(objects[i]));
                if (i != objects.length - 1) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
        if (obj instanceof List) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            List list = (List) obj;
            for (int i = 0; i < list.size(); i++) {
                stringBuilder.append(toString(list.get(i)));
                if (i != list.size() - 1) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
        if (obj instanceof Map) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{");
            Map map = (Map) obj;
            int i = 0;
            for (Object key : map.keySet()) {
                stringBuilder.append(toString(key));
                stringBuilder.append("=");
                stringBuilder.append(toString(map.get(key)));
                if (i != map.size() - 1) {
                    stringBuilder.append(", ");
                }
                i++;
            }
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
        if (obj instanceof AbstractConfig) {
            return obj.toString();
        }
        return obj.getClass() + "@" + Integer.toHexString(System.identityHashCode(obj));
    }
}
