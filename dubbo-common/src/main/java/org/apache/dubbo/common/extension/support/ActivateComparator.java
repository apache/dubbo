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
import org.apache.dubbo.common.extension.ExtensionDirector;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrderComparator
 */
public class ActivateComparator implements Comparator<Class<?>> {

    private final List<ExtensionDirector> extensionDirectors;
    private final Map<Class<?>, ActivateInfo> activateInfoMap = new ConcurrentHashMap<>();

    public ActivateComparator(ExtensionDirector extensionDirector) {
        extensionDirectors = new ArrayList<>();
        extensionDirectors.add(extensionDirector);
    }

    public ActivateComparator(List<ExtensionDirector> extensionDirectors) {
        this.extensionDirectors = extensionDirectors;
    }

    @Override
    public int compare(Class o1, Class o2) {
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

        Class<?> inf = findSpi(o1);

        ActivateInfo a1 = parseActivate(o1);
        ActivateInfo a2 = parseActivate(o2);

        if ((a1.applicableToCompare() || a2.applicableToCompare()) && inf != null) {
            if (a1.applicableToCompare()) {
                String n2 = null;
                for (ExtensionDirector director : extensionDirectors) {
                    ExtensionLoader<?> extensionLoader = director.getExtensionLoader(inf);
                    n2 = extensionLoader.getExtensionName(o2);
                    if (n2 != null) {
                        break;
                    }
                }

                if (a1.isLess(n2)) {
                    return -1;
                }

                if (a1.isMore(n2)) {
                    return 1;
                }
            }

            if (a2.applicableToCompare()) {
                String n1 = null;
                for (ExtensionDirector director : extensionDirectors) {
                    ExtensionLoader<?> extensionLoader = director.getExtensionLoader(inf);
                    n1 = extensionLoader.getExtensionName(o1);
                    if (n1 != null) {
                        break;
                    }
                }

                if (a2.isLess(n1)) {
                    return 1;
                }

                if (a2.isMore(n1)) {
                    return -1;
                }
            }

            return a1.order > a2.order ? 1 : -1;
        }

        // In order to avoid the problem of inconsistency between the loading order of two filters
        // in different loading scenarios without specifying the order attribute of the filter,
        // when the order is the same, compare its filterName
        if (a1.order > a2.order) {
            return 1;
        } else if (a1.order == a2.order) {
            return o1.getSimpleName().compareTo(o2.getSimpleName()) > 0 ? 1 : -1;
        } else {
            return -1;
        }
    }

    private Class<?> findSpi(Class<?> clazz) {
        if (clazz.getInterfaces().length == 0) {
            return null;
        }

        for (Class<?> intf : clazz.getInterfaces()) {
            if (intf.isAnnotationPresent(SPI.class)) {
                return intf;
            }
            Class<?> result = findSpi(intf);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    private ActivateInfo parseActivate(Class<?> clazz) {
        ActivateInfo info = activateInfoMap.get(clazz);
        if (info != null) {
            return info;
        }
        info = new ActivateInfo();
        if (clazz.isAnnotationPresent(Activate.class)) {
            Activate activate = clazz.getAnnotation(Activate.class);
            info.before = activate.before();
            info.after = activate.after();
            info.order = activate.order();
        } else if (clazz.isAnnotationPresent(com.alibaba.dubbo.common.extension.Activate.class)) {
            com.alibaba.dubbo.common.extension.Activate activate = clazz.getAnnotation(
                com.alibaba.dubbo.common.extension.Activate.class);
            info.before = activate.before();
            info.after = activate.after();
            info.order = activate.order();
        } else {
            info.order = 0;
        }
        activateInfoMap.put(clazz, info);
        return info;
    }

    private static class ActivateInfo {
        private String[] before;
        private String[] after;
        private int order;

        private boolean applicableToCompare() {
            return ArrayUtils.isNotEmpty(before) || ArrayUtils.isNotEmpty(after);
        }

        private boolean isLess(String name) {
            return Arrays.asList(before).contains(name);
        }

        private boolean isMore(String name) {
            return Arrays.asList(after).contains(name);
        }
    }
}
