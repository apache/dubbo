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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.apache.dubbo.common.function.Predicates.filterFirst;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.getDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.matchParameterTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;

/**
 * The utilities class for method in the package "javax.lang.model."
 *
 * @since 2.7.5
 */
public interface MethodUtils {

    static List<ExecutableElement> getDeclaredMethods(TypeMirror type) {
        return methodsIn(getDeclaredMembers(type));
    }

    static List<ExecutableElement> getAllDeclaredMethods(TypeMirror type) {
        return getHierarchicalTypes(type)
                .stream()
                .map(MethodUtils::getDeclaredMethods)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    static ExecutableElement findMethod(TypeMirror type, String methodName, Type oneParameterType, Type... otherParameterTypes) {
        List<Type> parameterTypes = new LinkedList<>();
        parameterTypes.add(oneParameterType);
        parameterTypes.addAll(asList(otherParameterTypes));
        return findMethod(type, methodName, parameterTypes.stream().map(Type::getTypeName).toArray(String[]::new));
    }

    static ExecutableElement findMethod(TypeMirror type, String methodName, CharSequence... parameterTypes) {
        return filterFirst(getAllDeclaredMethods(type),
                method -> methodName.equals(method.getSimpleName().toString()),
                method -> matchParameterTypes(method.getParameters(), parameterTypes)
        );
    }
}
