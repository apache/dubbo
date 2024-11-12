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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

public final class TypeParameterMeta extends ParameterMeta {

    private final Type type;

    public TypeParameterMeta(RestToolKit toolKit, Type type) {
        super(toolKit, null);
        this.type = type;
    }

    public TypeParameterMeta(Type type) {
        super(null, null);
        this.type = type;
    }

    @Override
    public Class<?> getType() {
        return TypeUtils.getActualType(type);
    }

    @Override
    public Type getGenericType() {
        return type;
    }

    @Override
    protected AnnotatedElement getAnnotatedElement() {
        return getActualType();
    }
}
