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
package org.apache.dubbo.xds.security.authz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO FIX
public class MapPathUtil {

    public static void putByPath(String value, List<String> mapPath, Map<String, List> map) {
        Map<String, List> currentMap = map;
        for (String s : mapPath) {
            currentMap = (Map<String, List>) currentMap.computeIfAbsent(s, v -> {
                List l = new ArrayList();
                l.add(new HashMap<>());
                return l;
            });
        }
        if (!mapPath.isEmpty()) {
            currentMap.get(mapPath.get(mapPath.size() - 1)).add(value);
        }
    }

    public static List<String> getByPath(List<String> path, Map<String, List> map) {
        Map<String, List> currentMap = map;
        for (int i = 0; i < path.size() - 1; i++) {
            Object nextMap = currentMap.get(path.get(i));
            if (nextMap instanceof Map) {
                currentMap = (Map<String, List>) nextMap;
            } else {
                return null;
            }
        }

        if (!path.isEmpty()) {
            return (List<String>) currentMap.get(path.get(path.size() - 1));
        }
        return null;
    }

    public static boolean isValid(List<String> valuesToValidate, List<String> ruleValues) {
        // TODO
        for (int i = 0; i < ruleValues.size(); i++) {
            for (int j = 0; j < valuesToValidate.size(); j++) {
                if (valuesToValidate.get(i).equals(ruleValues.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }
}
