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
package org.apache.dubbo.common.serialize.hessian2;

import java.io.IOException;

import com.alibaba.com.caucho.hessian.io.AbstractDeserializer;
import com.alibaba.com.caucho.hessian.io.AbstractHessianInput;

/**
 * Deserializer for a Kotlin {@code object} (and {@code data object}) declaration.
 *
 * <p>Kotlin guarantees that such a declaration has exactly one instance per class loader, exposed as
 * a {@code public static final INSTANCE} field, and gives it a private constructor so that no other
 * instance can be created. The default bean deserializers do not honour that: they allocate a fresh
 * instance (bypassing the private constructor) and populate its fields, so a value that crosses the
 * wire is no longer reference-equal to the singleton. For a plain {@code object}, whose
 * {@code equals} is inherited identity comparison, that also breaks {@code ==} and therefore
 * silently changes which branch a Kotlin {@code when} selects.
 *
 * <p>This deserializer resolves the declared singleton instead, mirroring how
 * {@code EnumDeserializer} resolves an enum constant by name: the serialized field data is read from
 * the stream so that the stream position stays correct, then discarded, and the singleton is
 * returned. Discarding any transmitted state is deliberate and matches enum behaviour — the identity
 * guarantee is the property being preserved.
 */
public class KotlinObjectDeserializer extends AbstractDeserializer {

    private final Class<?> type;
    private final Object instance;

    public KotlinObjectDeserializer(Class<?> type, Object instance) {
        this.type = type;
        this.instance = instance;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public Object readMap(AbstractHessianInput in) throws IOException {
        while (!in.isEnd()) {
            // Read and discard: the singleton's state is authoritative, as for an enum constant.
            in.readObject();
            in.readObject();
        }
        in.readMapEnd();
        in.addRef(instance);
        return instance;
    }

    @Override
    public Object readObject(AbstractHessianInput in, Object[] fields) throws IOException {
        String[] fieldNames = (String[]) fields;
        for (int i = 0; i < fieldNames.length; i++) {
            in.readObject();
        }
        in.addRef(instance);
        return instance;
    }
}
