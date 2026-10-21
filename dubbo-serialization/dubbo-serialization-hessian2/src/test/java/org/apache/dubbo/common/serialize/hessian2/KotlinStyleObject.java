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

import java.io.Serializable;

/**
 * Byte-compatible stand-in for a Kotlin {@code object} declaration: final class, private
 * constructor, {@code public static final INSTANCE} field of its own type, and the
 * {@code kotlin.Metadata} annotation the Kotlin compiler emits. Written in Java so that the module
 * needs no Kotlin compiler; the detection only inspects these bytecode features.
 *
 * <p>Carries one instance field, because a Kotlin {@code object} may hold properties and the
 * deserializer has to read that serialized field data to keep the stream position correct.
 */
@kotlin.Metadata
public final class KotlinStyleObject implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final KotlinStyleObject INSTANCE = new KotlinStyleObject();

    private String state = "initial";

    private KotlinStyleObject() {}

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
