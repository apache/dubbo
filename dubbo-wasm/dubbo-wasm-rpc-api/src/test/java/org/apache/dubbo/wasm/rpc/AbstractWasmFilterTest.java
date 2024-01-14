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
package org.apache.dubbo.wasm.rpc;

import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.HashMap;
import java.util.Map;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractWasmFilterTest {

    @Test
    void test() {
        Assertions.assertThrowsExactly(NullPointerException.class, () -> {
            try (RustFilter filter = new RustFilter()) {
                filter.invoke(null, null);
            }
        });
    }

    private static class RustFilter extends AbstractWasmFilter {
        @Override
        protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store) {
            Map<String, Func> funcMap = new HashMap<>();
            funcMap.put("hello_java", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, argId -> {
                // Argument argument = ARGUMENTS.get(argId);
                System.err.println("hello from java " + argId);
                return 1L;
            }));
            return funcMap;
        }

        @Override
        protected Long getArgumentId(Invoker<?> invoker, Invocation invocation) {
            return 0L;
        }
    }
}
