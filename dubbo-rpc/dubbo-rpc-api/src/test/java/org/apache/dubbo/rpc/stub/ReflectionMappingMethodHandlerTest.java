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

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link ReflectionMappingMethodHandler}
 * This test verifies the handler's ability to invoke arbitrary methods via reflection,
 * without needing a direct dependency on the @Mapping annotation itself.
 */
class ReflectionMappingMethodHandlerTest {

    static class TestService {
        public String hello(String name) {
            return "Hello " + name;
        }

        public CompletableFuture<String> helloAsync(String name) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.complete("Hello Async " + name);
            return future;
        }

        public void voidMethod(String input) {
            if ("nullInput".equals(input)) {
                throw new NullPointerException("Input was null");
            }
        }

        public String throwBusinessException(String input) {
            throw new BusinessException("Business exception thrown: " + input);
        }
    }

    static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }

    private TestService testService;
    private Method helloMethod;
    private Method helloAsyncMethod;
    private Method voidMethod;
    private Method throwBusinessExceptionMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        testService = new TestService();
        helloMethod = TestService.class.getMethod("hello", String.class);
        helloAsyncMethod = TestService.class.getMethod("helloAsync", String.class);
        voidMethod = TestService.class.getMethod("voidMethod", String.class);
        throwBusinessExceptionMethod = TestService.class.getMethod("throwBusinessException", String.class);
    }

    @Test
    void testSyncMethodInvoke() throws ExecutionException, InterruptedException {
        ReflectionMappingMethodHandler handler = new ReflectionMappingMethodHandler(testService, helloMethod);
        Object[] args = new Object[] {"World"};
        CompletableFuture<?> future = handler.invoke(args);
        Assertions.assertEquals("Hello World", future.get());
    }

    @Test
    void testAsyncMethodInvoke() throws ExecutionException, InterruptedException {
        ReflectionMappingMethodHandler handler = new ReflectionMappingMethodHandler(testService, helloAsyncMethod);
        Object[] args = new Object[] {"Async"};
        CompletableFuture<?> future = handler.invoke(args);
        Assertions.assertEquals("Hello Async Async", future.get());
    }

    @Test
    void testVoidMethodInvoke() throws ExecutionException, InterruptedException {
        ReflectionMappingMethodHandler handler = new ReflectionMappingMethodHandler(testService, voidMethod);
        Object[] args = new Object[] {"test"};
        CompletableFuture<?> future = handler.invoke(args);
        Assertions.assertNull(future.get());
    }

    @Test
    void testBusinessExceptionMethodInvoke() {
        ReflectionMappingMethodHandler handler = new ReflectionMappingMethodHandler(testService, throwBusinessExceptionMethod);
        Object[] args = new Object[] {"test-error"};
        CompletableFuture<?> future = handler.invoke(args);

        ExecutionException ex = Assertions.assertThrows(ExecutionException.class, future::get);
        Throwable cause = ex.getCause();
        Assertions.assertInstanceOf(BusinessException.class, cause);
        Assertions.assertEquals("Business exception thrown: test-error", cause.getMessage());
    }

    @Test
    void testOtherRuntimeExceptionMethodInvoke() {
        ReflectionMappingMethodHandler handler = new ReflectionMappingMethodHandler(testService, voidMethod);
        Object[] args = new Object[] {"nullInput"}; // This will cause voidMethod to throw NullPointerException
        CompletableFuture<?> future = handler.invoke(args);

        ExecutionException ex = Assertions.assertThrows(ExecutionException.class, future::get);
        Throwable cause = ex.getCause();
        Assertions.assertInstanceOf(NullPointerException.class, cause); // The original NPE
        Assertions.assertEquals("Input was null", cause.getMessage());
    }


    @Test
    void testIllegalArgumentExceptionFromReflection() {
        ReflectionMappingMethodHandler handler = new ReflectionMappingMethodHandler(testService, helloMethod);
        Object[] args = new Object[] {};

        CompletableFuture<?> future = handler.invoke(args);
        ExecutionException ex = Assertions.assertThrows(ExecutionException.class, future::get);
        Throwable cause = ex.getCause();
        Assertions.assertInstanceOf(RpcException.class, cause);
        Assertions.assertTrue(cause.getMessage().contains("Reflection call failed for @Mapping method 'hello'"));
        Assertions.assertInstanceOf(IllegalArgumentException.class, cause.getCause());
    }
}
