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

import java.lang.reflect.Field;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.dubbo.common.utils.FieldUtils.getFieldValue;

/**
 * The constant field value {@link Predicate} for the specified {@link Class}
 *
 * @see Predicate
 * @since 2.7.8
 */
public class StringConstantFieldValuePredicate implements Predicate<String> {

    private final Set<String> constantFieldValues;

    public StringConstantFieldValuePredicate(Class<?> targetClass) {
        this.constantFieldValues = getConstantFieldValues(targetClass);
    }

    public static Predicate<String> of(Class<?> targetClass) {
        return new StringConstantFieldValuePredicate(targetClass);
    }

    private Set<String> getConstantFieldValues(Class<?> targetClass) {
        return Stream.of(targetClass.getFields())
                .filter(f -> isStatic(f.getModifiers()))         // static
                .filter(f -> isPublic(f.getModifiers()))         // public
                .filter(f -> isFinal(f.getModifiers()))          // final
                .map(this::getConstantValue)
                .filter(v -> v instanceof String)                // filters String type
                .map(String.class::cast)                         // Casts String type
                .collect(Collectors.toSet());
    }

    @Override
    public boolean test(String s) {
        return constantFieldValues.contains(s);
    }

    private Object getConstantValue(Field field) {
        return getFieldValue(null, field);
    }
}
