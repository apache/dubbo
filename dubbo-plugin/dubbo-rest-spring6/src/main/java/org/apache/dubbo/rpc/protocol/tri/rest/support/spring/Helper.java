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
package org.apache.dubbo.rpc.protocol.tri.rest.support.spring;

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.SpringVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ValueConstants;

final class Helper {

    public static boolean IS_SPRING_6;

    private static Method getStatusCode;
    private static Method value;

    private Helper() {}

    static {
        try {
            String version = SpringVersion.getVersion();
            IS_SPRING_6 = StringUtils.hasLength(version) && version.charAt(0) >= '6';
        } catch (Throwable ignored) {
        }
    }

    public static boolean isRequired(AnnotationMeta<Annotation> annotation) {
        return annotation.getBoolean("required");
    }

    public static String defaultValue(AnnotationMeta<Annotation> annotation) {
        return defaultValue(annotation, "defaultValue");
    }

    public static String defaultValue(AnnotationMeta<Annotation> annotation, String name) {
        return defaultValue(annotation.getString(name));
    }

    public static String defaultValue(String value) {
        return ValueConstants.DEFAULT_NONE.equals(value) ? null : value;
    }

    public static int getStatusCode(ResponseEntity<?> entity) {
        if (IS_SPRING_6) {
            try {
                if (getStatusCode == null) {
                    getStatusCode = ResponseEntity.class.getMethod("getStatusCode");
                    value = getStatusCode.getReturnType().getMethod("value");
                }
                return (Integer) value.invoke(getStatusCode.invoke(entity));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return entity.getStatusCode().value();
    }
}
