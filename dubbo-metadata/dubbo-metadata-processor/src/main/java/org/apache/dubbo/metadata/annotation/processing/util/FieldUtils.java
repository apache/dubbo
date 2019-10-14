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
package org.apache.dubbo.metadata.annotation.processing.util;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static javax.lang.model.util.ElementFilter.fieldsIn;
import static org.apache.dubbo.common.function.Predicates.filterAll;
import static org.apache.dubbo.common.function.Predicates.filterFirst;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.getDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;

/**
 * The utilities class for the field in the package "javax.lang.model."
 *
 * @since 2.7.5
 */
public interface FieldUtils {

    static List<VariableElement> getDeclaredFields(TypeMirror type, Predicate<VariableElement>... fieldFilters) {
        return filterAll(fieldsIn(getDeclaredMembers(type)), fieldFilters);
    }

    static List<VariableElement> getAllDeclaredFields(TypeMirror type, Predicate<VariableElement>... fieldFilters) {
        return getHierarchicalTypes(type)
                .stream()
                .map(t -> getDeclaredFields(t, fieldFilters))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    static VariableElement getDeclaredField(TypeMirror type, String fieldName) {
        return filterFirst(getDeclaredFields(type, field -> fieldName.equals(field.getSimpleName().toString())));
    }

    static VariableElement findField(TypeMirror type, String fieldName) {
        return filterFirst(getAllDeclaredFields(type, field -> fieldName.equals(field.getSimpleName().toString())));
    }
}
