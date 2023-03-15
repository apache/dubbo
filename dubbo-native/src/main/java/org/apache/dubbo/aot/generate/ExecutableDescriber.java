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


import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A describer that describes the need for reflection on a {@link Executable}.
 */
public class ExecutableDescriber extends MemberDescriber {

    private final List<String> parameterTypes;

    private final ExecutableMode mode;


    public ExecutableDescriber(Constructor<?> constructor, ExecutableMode mode) {
        this("<init>", Arrays.stream(constructor.getParameterTypes()).map(Class::getName).collect(Collectors.toList()),mode);
    }

    public ExecutableDescriber(String name, List<String> parameterTypes, ExecutableMode mode) {
        super(name);
        this.parameterTypes = parameterTypes;
        this.mode = mode;
    }


    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public ExecutableMode getMode() {
        return mode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExecutableDescriber that = (ExecutableDescriber) o;
        return Objects.equals(parameterTypes, that.parameterTypes) && mode == that.mode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterTypes, mode);
    }
}
