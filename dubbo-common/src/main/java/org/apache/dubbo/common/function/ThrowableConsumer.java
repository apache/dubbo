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
package org.apache.dubbo.common.function;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link Consumer} with {@link Throwable}
 *
 * @param <T> the source type
 * @see Function
 * @see Throwable
 * @since 2.7.5
 */
@FunctionalInterface
public interface ThrowableConsumer<T> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @throws Throwable if met with any error
     */
    void accept(T t) throws Throwable;

    /**
     * Executes {@link ThrowableConsumer}
     *
     * @param t the function argument
     * @throws RuntimeException wrappers {@link Throwable}
     */
    default void execute(T t) throws RuntimeException {
        try {
            accept(t);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Executes {@link ThrowableConsumer}
     *
     * @param t        the function argument
     * @param consumer {@link ThrowableConsumer}
     * @param <T>      the source type
     */
    static <T> void execute(T t, ThrowableConsumer<T> consumer) {
        consumer.execute(t);
    }
}
