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
package org.apache.dubbo.aot.generate;

import org.apache.dubbo.aot.api.ExecutableDescriber;
import org.apache.dubbo.aot.api.MemberCategory;
import org.apache.dubbo.aot.api.TypeDescriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.dubbo.aot.api.ExecutableMode.INVOKE;

public class ReflectConfigMetadataRepository {

    List<TypeDescriber> types;

    public ReflectConfigMetadataRepository() {
        this.types = new ArrayList<>();
    }

    protected ReflectConfigMetadataRepository registerSpiExtensionType(List<Class<?>> classes) {
        types.addAll(classes.stream()
                .filter(Objects::nonNull)
                .map(this::buildTypeDescriberWithConstructor)
                .collect(Collectors.toList()));
        return this;
    }

    protected ReflectConfigMetadataRepository registerAdaptiveType(List<Class<?>> classes) {
        types.addAll(classes.stream()
                .filter(Objects::nonNull)
                .map(this::buildTypeDescriberWithConstructor)
                .collect(Collectors.toList()));
        return this;
    }

    protected ReflectConfigMetadataRepository registerBeanType(List<Class<?>> classes) {
        types.addAll(classes.stream()
                .filter(Objects::nonNull)
                .map(this::buildTypeDescriberWithConstructor)
                .collect(Collectors.toList()));
        return this;
    }

    protected ReflectConfigMetadataRepository registerConfigType(List<Class<?>> classes) {
        types.addAll(classes.stream()
                .filter(Objects::nonNull)
                .map(this::buildTypeDescriberWithConstructor)
                .collect(Collectors.toList()));
        return this;
    }

    private TypeDescriber buildTypeDescriberWithConstructor(Class<?> c) {
        Set<ExecutableDescriber> constructors = Arrays.stream(c.getConstructors())
                .map((constructor) -> new ExecutableDescriber(constructor, INVOKE))
                .collect(Collectors.toSet());
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_PUBLIC_METHODS);
        return new TypeDescriber(c.getName(), null, new HashSet<>(), constructors, new HashSet<>(), memberCategories);
    }

    protected ReflectConfigMetadataRepository registerFieldType(List<Class<?>> classes) {
        types.addAll(classes.stream()
                .filter(Objects::nonNull)
                .map(this::buildTypeDescriberWithField)
                .collect(Collectors.toList()));
        return this;
    }

    private TypeDescriber buildTypeDescriberWithField(Class<?> c) {
        Set<ExecutableDescriber> constructors = Arrays.stream(c.getConstructors())
                .map((constructor) -> new ExecutableDescriber(constructor, INVOKE))
                .collect(Collectors.toSet());
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.PUBLIC_FIELDS);
        return new TypeDescriber(c.getName(), null, new HashSet<>(), constructors, new HashSet<>(), memberCategories);
    }

    public void registerTypeDescriber(List<TypeDescriber> typeDescribers) {
        types.addAll(typeDescribers.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    public List<TypeDescriber> getTypes() {
        return types;
    }
}
