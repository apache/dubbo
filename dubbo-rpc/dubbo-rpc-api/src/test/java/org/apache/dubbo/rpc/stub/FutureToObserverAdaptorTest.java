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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.fail;

class FutureToObserverAdaptorTest {

    @Test
    void testAdapt() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<String> done = CompletableFuture.completedFuture("a");
        FutureToObserverAdaptor<String> adaptor = new FutureToObserverAdaptor<>(done);
        try {
            adaptor.onNext("teet");
            fail();
        } catch (IllegalStateException e) {
            // pass
        }

        CompletableFuture<String> cancel = new CompletableFuture<>();
        cancel.cancel(true);
        FutureToObserverAdaptor<String> adaptor1 = new FutureToObserverAdaptor<>(cancel);
        try {
            adaptor1.onNext("teet");
            fail();
        } catch (IllegalStateException e) {
            // pass
        }

        CompletableFuture<String> exc = new CompletableFuture<>();
        exc.completeExceptionally(new IllegalStateException("Test"));
        FutureToObserverAdaptor<String> adaptor2 = new FutureToObserverAdaptor<>(exc);
        try {
            adaptor2.onNext("teet");
            fail();
        } catch (IllegalStateException e) {
            // pass
        }

        CompletableFuture<String> success = new CompletableFuture<>();
        FutureToObserverAdaptor<String> adaptor3 = new FutureToObserverAdaptor<>(success);
        adaptor3.onNext("test");
        adaptor3.onCompleted();
        Assertions.assertEquals("test", success.get(1, TimeUnit.SECONDS));

        CompletableFuture<String> exc2 = new CompletableFuture<>();
        FutureToObserverAdaptor<String> adaptor4 = new FutureToObserverAdaptor<>(exc2);
        adaptor4.onError(new IllegalStateException("test"));
        try {
            exc2.get();
            fail();
        } catch (ExecutionException e) {
            Assertions.assertTrue(e.getCause() instanceof IllegalStateException);
        }

        CompletableFuture<String> complete = new CompletableFuture<>();
        FutureToObserverAdaptor<String> adaptor5 = new FutureToObserverAdaptor<>(complete);
        try {
            adaptor5.onCompleted();
            fail();
        } catch (IllegalStateException e) {
            // pass
        }
    }
}
