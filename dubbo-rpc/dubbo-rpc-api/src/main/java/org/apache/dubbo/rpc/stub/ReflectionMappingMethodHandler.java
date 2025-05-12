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
package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.rpc.RpcException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class ReflectionMappingMethodHandler implements StubMethodHandler<Object[], Object> {
    private final Object serviceInstance;
    private final Method method;

    public ReflectionMappingMethodHandler(Object serviceInstance, Method method) {
        this.serviceInstance = serviceInstance;
        this.method = method;
        this.method.setAccessible(true);
    }

    @Override
    public CompletableFuture<?> invoke(Object[] arguments) {
        try {
            Object result = method.invoke(serviceInstance, arguments);

            if (result instanceof CompletableFuture) {
                return (CompletableFuture<?>) result;
            }
            return CompletableFuture.completedFuture(result);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            CompletableFuture<?> future = new CompletableFuture<>();
            future.completeExceptionally(targetException);
            return future;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            CompletableFuture<?> future = new CompletableFuture<>();
            future.completeExceptionally(new RpcException(
                    "Reflection call failed for @Mapping method '" + method.getName() + "': " + e.getMessage(), e));
            return future;
        } catch (Throwable t) {
            CompletableFuture<?> future = new CompletableFuture<>();
            future.completeExceptionally(new RpcException(
                    "Unexpected error invoking @Mapping method '" + method.getName() + "': " + t.getMessage(), t));
            return future;
        }
    }
}
