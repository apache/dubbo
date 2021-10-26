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

import java.util.HashMap;
import java.util.Map;

/**
 * Map tools
 */
public class MapUtils {

    /**
     * switch Map<String, Object> to Map<String, String>
     *
     * If the value of the original Map is not of type String, then toString() of value will be called
     *
     * @param originMap
     * @return
     */
    public static Map<String, String> objectToStringMap(Map<String, Object> originMap) {
        Map<String, String> newStrMap = new HashMap<>();

        if (originMap == null) {
            return newStrMap;
        }

        for (Map.Entry<String, Object> entry : originMap.entrySet()) {
            String stringValue = convertToString(entry.getValue());
            if (stringValue != null) {
                newStrMap.put(entry.getKey(), stringValue);
            }
        }

        return newStrMap;
    }

    /**
     * use {@link Object#toString()} switch Obj to String
     *
     * @param obj
     * @return
     */
    private static String convertToString(Object obj) {
        if (obj == null) {
            return null;
        } else {
            return obj.toString();
        }
    }
}
