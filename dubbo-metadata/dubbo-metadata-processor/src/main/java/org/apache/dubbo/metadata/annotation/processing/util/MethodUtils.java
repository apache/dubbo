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


import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.util.ElementFilter.methodsIn;
import static org.apache.dubbo.common.function.Predicates.EMPTY_ARRAY;
import static org.apache.dubbo.common.function.Streams.filter;
import static org.apache.dubbo.common.function.Streams.filterAll;
import static org.apache.dubbo.common.function.Streams.filterFirst;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.getDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.isPublicNonStatic;
import static org.apache.dubbo.metadata.annotation.processing.util.MemberUtils.matchParameterTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofDeclaredType;

/**
 * The utilities class for method in the package "javax.lang.model."
 *
 * @since 2.7.6
 */
public interface MethodUtils {

    static List<ExecutableElement> getDeclaredMethods(TypeElement type, Predicate<ExecutableElement>... methodFilters) {
        return type == null ? emptyList() : getDeclaredMethods(type.asType(), methodFilters);
    }

    static List<ExecutableElement> getDeclaredMethods(TypeMirror type, Predicate<ExecutableElement>... methodFilters) {
        return filterAll(methodsIn(getDeclaredMembers(type)), methodFilters);
    }

    static List<ExecutableElement> getAllDeclaredMethods(TypeElement type, Predicate<ExecutableElement>... methodFilters) {
        return type == null ? emptyList() : getAllDeclaredMethods(type.asType(), methodFilters);
    }

    static List<ExecutableElement> getAllDeclaredMethods(TypeElement type) {
        return getAllDeclaredMethods(type, EMPTY_ARRAY);
    }

    static List<ExecutableElement> getAllDeclaredMethods(TypeMirror type, Predicate<ExecutableElement>... methodFilters) {
        return getHierarchicalTypes(type)
                .stream()
                .map(t -> getDeclaredMethods(t, methodFilters))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    static List<ExecutableElement> getAllDeclaredMethods(TypeMirror type) {
        return getAllDeclaredMethods(type, EMPTY_ARRAY);
    }

    static List<ExecutableElement> getAllDeclaredMethods(TypeElement type, Type... excludedTypes) {
        return type == null ? emptyList() : getAllDeclaredMethods(type.asType(), excludedTypes);
    }

    static List<ExecutableElement> getAllDeclaredMethods(TypeMirror type, Type... excludedTypes) {
        return getHierarchicalTypes(type, excludedTypes)
                .stream()
                .map(t -> getDeclaredMethods(t))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    static List<ExecutableElement> getPublicNonStaticMethods(TypeElement type, Type... excludedTypes) {
        return getPublicNonStaticMethods(ofDeclaredType(type), excludedTypes);
    }

    static List<ExecutableElement> getPublicNonStaticMethods(TypeMirror type, Type... excludedTypes) {
        return filter(getAllDeclaredMethods(type, excludedTypes), MethodUtils::isPublicNonStaticMethod);
    }

    static boolean isMethod(ExecutableElement method) {
        return method != null && METHOD.equals(method.getKind());
    }

    static boolean isPublicNonStaticMethod(ExecutableElement method) {
        return isMethod(method) && isPublicNonStatic(method);
    }

    static ExecutableElement findMethod(TypeElement type, String methodName, Type oneParameterType, Type... otherParameterTypes) {
        return type == null ? null : findMethod(type.asType(), methodName, oneParameterType, otherParameterTypes);
    }

    static ExecutableElement findMethod(TypeMirror type, String methodName, Type oneParameterType, Type... otherParameterTypes) {
        List<Type> parameterTypes = new LinkedList<>();
        parameterTypes.add(oneParameterType);
        parameterTypes.addAll(asList(otherParameterTypes));
        return findMethod(type, methodName, parameterTypes.stream().map(Type::getTypeName).toArray(String[]::new));
    }

    static ExecutableElement findMethod(TypeElement type, String methodName, CharSequence... parameterTypes) {
        return type == null ? null : findMethod(type.asType(), methodName, parameterTypes);
    }

    static ExecutableElement findMethod(TypeMirror type, String methodName, CharSequence... parameterTypes) {
        return filterFirst(getAllDeclaredMethods(type),
                method -> methodName.equals(method.getSimpleName().toString()),
                method -> matchParameterTypes(method.getParameters(), parameterTypes)
        );
    }

    static ExecutableElement getOverrideMethod(ProcessingEnvironment processingEnv, TypeElement type,
                                               ExecutableElement declaringMethod) {
        Elements elements = processingEnv.getElementUtils();
        return filterFirst(getAllDeclaredMethods(type), method -> elements.overrides(method, declaringMethod, type));
    }


    static String getMethodName(ExecutableElement method) {
        return method == null ? null : method.getSimpleName().toString();
    }

    static String getReturnType(ExecutableElement method) {
        return method == null ? null : TypeUtils.toString(method.getReturnType());
    }

    static String[] getMethodParameterTypes(ExecutableElement method) {
        return method == null ?
                new String[0] :
                method.getParameters()
                        .stream()
                        .map(VariableElement::asType)
                        .map(TypeUtils::toString)
                        .toArray(String[]::new);
    }
}
