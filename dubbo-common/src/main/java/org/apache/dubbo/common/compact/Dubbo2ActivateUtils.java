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
package org.apache.dubbo.common.compact;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class Dubbo2ActivateUtils {
    private static final Class<? extends Annotation> ACTIVATE_CLASS;
    private static final Method GROUP_METHOD;
    private static final Method VALUE_METHOD;
    private static final Method BEFORE_METHOD;
    private static final Method AFTER_METHOD;
    private static final Method ORDER_METHOD;
    private static final Method ON_CLASS_METHOD;

    static {
        ACTIVATE_CLASS = loadClass();
        GROUP_METHOD = loadMethod("group");
        VALUE_METHOD = loadMethod("value");
        BEFORE_METHOD = loadMethod("before");
        AFTER_METHOD = loadMethod("after");
        ORDER_METHOD = loadMethod("order");
        ON_CLASS_METHOD = loadMethod("onClass");
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadClass() {
        try {
            Class<?> clazz = Class.forName("com.alibaba.dubbo.common.extension.Activate");
            if (clazz.isAnnotation()) {
                return (Class<? extends Annotation>) clazz;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isActivateLoaded() {
        return ACTIVATE_CLASS != null;
    }

    public static Class<? extends Annotation> getActivateClass() {
        return ACTIVATE_CLASS;
    }

    private static Method loadMethod(String name) {
        if (ACTIVATE_CLASS == null) {
            return null;
        }
        try {
            return ACTIVATE_CLASS.getMethod(name);
        } catch (Throwable e) {
            return null;
        }
    }

    public static String[] getGroup(Annotation annotation) {
        if (GROUP_METHOD == null) {
            return null;
        }
        try {
            Object result = GROUP_METHOD.invoke(annotation);
            if (result instanceof String[]) {
                return (String[]) result;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    public static String[] getValue(Annotation annotation) {
        if (VALUE_METHOD == null) {
            return null;
        }
        try {
            Object result = VALUE_METHOD.invoke(annotation);
            if (result instanceof String[]) {
                return (String[]) result;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    public static String[] getBefore(Annotation annotation) {
        if (BEFORE_METHOD == null) {
            return null;
        }
        try {
            Object result = BEFORE_METHOD.invoke(annotation);
            if (result instanceof String[]) {
                return (String[]) result;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    public static String[] getAfter(Annotation annotation) {
        if (AFTER_METHOD == null) {
            return null;
        }
        try {
            Object result = AFTER_METHOD.invoke(annotation);
            if (result instanceof String[]) {
                return (String[]) result;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }

    public static int getOrder(Annotation annotation) {
        if (ORDER_METHOD == null) {
            return 0;
        }
        try {
            Object result = ORDER_METHOD.invoke(annotation);
            if (result instanceof Integer) {
                return (Integer) result;
            } else {
                return 0;
            }
        } catch (Throwable e) {
            return 0;
        }
    }

    public static String[] getOnClass(Annotation annotation) {
        if (ON_CLASS_METHOD == null) {
            return null;
        }
        try {
            Object result = ON_CLASS_METHOD.invoke(annotation);
            if (result instanceof String[]) {
                return (String[]) result;
            } else {
                return null;
            }
        } catch (Throwable e) {
            return null;
        }
    }
}
