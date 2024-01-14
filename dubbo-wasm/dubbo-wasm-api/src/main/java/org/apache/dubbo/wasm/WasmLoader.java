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
package org.apache.dubbo.wasm;

import org.apache.dubbo.wasm.exception.WasmInitException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Memory;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.wasi.WasiCtx;
import io.github.kawamuray.wasmtime.wasi.WasiCtxBuilder;

/**
 * The WasmLoader aims to load wasm file and provide the wasm function to java,
 * also provide the java function to wasm if we need.
 */
public class WasmLoader implements AutoCloseable {

    private static final String IMPORT_WASM_MODULE_NAME = "dubbo";

    private static final String MEMORY_METHOD_NAME = "memory";

    protected final String wasmName;

    private final WasiCtx wasiCtx =
            new WasiCtxBuilder().inheritStdout().inheritStderr().build();

    /**
     * @see WasmFunctions#wrap
     * @see WasmFunctions#consumer
     * @see WasmFunctions#func
     */
    private final Store<Void> store = Store.withoutData(wasiCtx);

    private final Linker linker = new Linker(store.engine());

    /**
     * wasmCallJavaFuncName -> wasmCallJavaFunc
     */
    private final Map<String, Func> wasmCallJavaFuncMap = new HashMap<>();

    private final Module module;

    private Memory memRef;

    public WasmLoader() {
        this(null, null);
    }

    /**
     * This constructor is designed for classes that cannot extend WasmLoader.
     */
    public WasmLoader(Class<?> wasmClass, Map<String, Func> userWasmFunctionMap) {
        this.wasmName = (wasmClass != null ? wasmClass : this.getClass()).getName() + ".wasm";
        try {
            // locate `.wasm` lib.
            Path wasmPath = Paths.get(
                    WasmLoader.class.getClassLoader().getResource(wasmName).toURI());
            // Reads the WebAssembly module as bytes.
            byte[] wasmBytes = Files.readAllBytes(wasmPath);
            // Instantiates the WebAssembly module.
            if (Objects.nonNull(userWasmFunctionMap) && !userWasmFunctionMap.isEmpty()) {
                wasmCallJavaFuncMap.putAll(userWasmFunctionMap);
            }
            Map<String, Func> dubboWasmFunctionMap = initWasmCallJavaFunc(store);
            if (Objects.nonNull(dubboWasmFunctionMap) && !dubboWasmFunctionMap.isEmpty()) {
                wasmCallJavaFuncMap.putAll(dubboWasmFunctionMap);
            }
            this.module = Module.fromBinary(store.engine(), wasmBytes);
            WasiCtx.addToLinker(linker);
            // maybe need define many functions
            if (!wasmCallJavaFuncMap.isEmpty()) {
                wasmCallJavaFuncMap.forEach((funcName, wasmCallJavaFunc) ->
                        linker.define(store, IMPORT_WASM_MODULE_NAME, funcName, Extern.fromFunc(wasmCallJavaFunc)));
            }
            linker.module(store, "", module);
            // Let the `wasmCallJavaFunc` function to refer this as a placeholder of Memory because
            // we have to add the function as import before loading the module exporting Memory.
            this.getWasmExtern(MEMORY_METHOD_NAME).ifPresent(extern -> memRef = extern.memory());
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        } catch (URISyntaxException | IOException e) {
            throw new WasmInitException(e);
        }
    }

    protected Map<String, Func> initWasmCallJavaFunc(Store<Void> store) {
        return null;
    }

    public Optional<Extern> getWasmExtern(String wasiFuncName) {
        return linker.get(store, "", wasiFuncName);
    }

    public String getWasmName() {
        return wasmName;
    }

    public Store<Void> getStore() {
        return store;
    }

    /**
     * use this in wasmCallJavaFunc.
     */
    public ByteBuffer getBuffer() {
        return memRef.buffer(store);
    }

    @Override
    public void close() {
        this.wasiCtx.close();
        this.store.close();
        this.linker.close();
        if (!wasmCallJavaFuncMap.isEmpty()) {
            this.wasmCallJavaFuncMap.forEach((funcName, wasmCallJavaFunc) -> wasmCallJavaFunc.close());
        }
        this.module.close();
    }
}
