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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.getHierarchicalTypes;
import static org.apache.dubbo.metadata.annotation.processing.util.TypeUtils.ofTypeElement;

/**
 * The utilities class for the members in the package "javax.lang.model.", such as "field", "method", "constructor"
 *
 * @since 2.7.6
 */
public interface MemberUtils {

    static boolean matches(Element member, ElementKind kind) {
        return member == null || kind == null ? false : kind.equals(member.getKind());
    }

    static boolean isPublicNonStatic(Element member) {
        return hasModifiers(member, PUBLIC) && !hasModifiers(member, STATIC);
    }

    static boolean hasModifiers(Element member, Modifier... modifiers) {
        if (member == null || modifiers == null) {
            return false;
        }
        Set<Modifier> actualModifiers = member.getModifiers();
        for (Modifier modifier : modifiers) {
            if (!actualModifiers.contains(modifier)) {
                return false;
            }
        }
        return true;
    }

    static List<? extends Element> getDeclaredMembers(TypeMirror type) {
        TypeElement element = ofTypeElement(type);
        return element == null ? emptyList() : element.getEnclosedElements();
    }

    static List<? extends Element> getAllDeclaredMembers(TypeMirror type) {
        return getHierarchicalTypes(type)
                .stream()
                .map(MemberUtils::getDeclaredMembers)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    static boolean matchParameterTypes(List<? extends VariableElement> parameters, CharSequence... parameterTypes) {

        int size = parameters.size();

        if (size != parameterTypes.length) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            VariableElement parameter = parameters.get(i);
            if (!Objects.equals(parameter.asType().toString(), parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }
}
