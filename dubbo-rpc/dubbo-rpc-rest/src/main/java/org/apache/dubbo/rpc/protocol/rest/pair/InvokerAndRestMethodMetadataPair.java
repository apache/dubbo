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
package org.apache.dubbo.rpc.protocol.rest.pair;

import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.Invoker;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * for invoker & restMethodMetadata pair
 */
public class InvokerAndRestMethodMetadataPair {

    Invoker invoker;
    RestMethodMetadata restMethodMetadata;

    public InvokerAndRestMethodMetadataPair(Invoker invoker, RestMethodMetadata restMethodMetadata) {
        this.invoker = invoker;
        this.restMethodMetadata = restMethodMetadata;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public RestMethodMetadata getRestMethodMetadata() {
        return restMethodMetadata;
    }

    public static InvokerAndRestMethodMetadataPair pair(Invoker invoker, RestMethodMetadata restMethodMetadata) {
        return new InvokerAndRestMethodMetadataPair(invoker, restMethodMetadata);
    }

    /**
     * same interface  & same  method desc
     *
     * @param beforeMetadata
     * @return
     */
    public boolean compareServiceMethod(InvokerAndRestMethodMetadataPair beforeMetadata) {

        Class currentServiceInterface = this.invoker.getInterface();
        Class<?> beforeServiceInterface = beforeMetadata.getInvoker().getInterface();

        if (!currentServiceInterface.equals(beforeServiceInterface)) {
            return false;
        }

        Method beforeServiceMethod = beforeMetadata.getRestMethodMetadata().getReflectMethod();

        Method currentReflectMethod = this.restMethodMetadata.getReflectMethod();

        if (beforeServiceMethod.getName().equals(currentReflectMethod.getName()) // method name
                // method param types
                && Arrays.toString(beforeServiceMethod.getParameterTypes())
                        .equals(Arrays.toString(currentReflectMethod.getParameterTypes()))) {
            return true;
        }

        return false;
    }
}
