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
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionMethodDescriptor;
import org.apache.dubbo.rpc.model.StubMethodDescriptor;

public class MethodMetadata {

    private final Class<?>[] actualRequestTypes;

    private final Class<?> actualResponseType;

    private MethodMetadata(Class<?>[] actualRequestTypes, Class<?> actualResponseType) {
        this.actualRequestTypes = actualRequestTypes;
        this.actualResponseType = actualResponseType;
    }

    public Class<?>[] getActualRequestTypes() {
        return actualRequestTypes;
    }

    public Class<?> getActualResponseType() {
        return actualResponseType;
    }

    public static MethodMetadata fromMethodDescriptor(MethodDescriptor method) {
        if (method instanceof ReflectionMethodDescriptor) {
            return doResolveReflection((ReflectionMethodDescriptor) method);
        }
        if (method instanceof StubMethodDescriptor) {
            return doResolveStub((StubMethodDescriptor) method);
        }
        throw new IllegalStateException("Can not reach here");
    }

    private static MethodMetadata doResolveStub(StubMethodDescriptor method) {
        Class<?>[] actualRequestTypes = method.getParameterClasses();
        Class<?> actualResponseType = method.getReturnClass();
        return new MethodMetadata(actualRequestTypes, actualResponseType);
    }

    private static MethodMetadata doResolveReflection(ReflectionMethodDescriptor method) {
        Class<?>[] actualRequestTypes;
        Class<?> actualResponseType;
        switch (method.getRpcType()) {
            case CLIENT_STREAM:
            case BI_STREAM:
            case SERVER_STREAM:
                actualRequestTypes = method.getActualRequestTypes();
                actualResponseType = method.getActualResponseType();
                return new MethodMetadata(actualRequestTypes, actualResponseType);
            case UNARY:
                actualRequestTypes = method.getParameterClasses();
                actualResponseType = (Class<?>) method.getReturnTypes()[0];
                return new MethodMetadata(actualRequestTypes, actualResponseType);
        }
        throw new IllegalStateException("Can not reach here");
    }
}
