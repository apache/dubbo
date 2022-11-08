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

package org.apache.dubbo.common.utils;

import java.util.function.Supplier;

public class CacheableSupplier<T> implements Supplier<T> {

    private volatile T object;

    private final Supplier<T> supplier;

    public CacheableSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> CacheableSupplier<T> newSupplier(Supplier<T> supplier) {
        return new CacheableSupplier<>(supplier);
    }

    @Override
    public T get() {
        if (this.object == null) {
            this.object = supplier.get();
        }
        return object;
    }
}
