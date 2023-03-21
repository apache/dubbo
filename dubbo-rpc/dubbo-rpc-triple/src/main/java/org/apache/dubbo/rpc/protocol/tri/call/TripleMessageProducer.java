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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.function.ThrowableSupplier;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

class TripleMessageProducer implements ClientCall.MessageProducer {

    private final ThrowableSupplier<Object> throwableSupplier;

    private Consumer<Throwable> throwableConsumer = (throwable) -> {
    };

    private TripleMessageProducer(ThrowableSupplier<Object> throwableSupplier) {
        this.throwableSupplier = throwableSupplier;
    }

    public TripleMessageProducer onException(Consumer<Throwable> throwableConsumer) {
        this.throwableConsumer = throwableConsumer;
        return this;
    }

    @Override
    public Object getMessage() {
        try {
            return throwableSupplier.get();
        } catch (Throwable t) {
            if (throwableConsumer != null) {
                throwableConsumer.accept(t);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            if (t instanceof InvocationTargetException) {
                Throwable targetException = ((InvocationTargetException) t).getTargetException();
                throw new RuntimeException(targetException);
            }
            throw new RuntimeException(t);
        }
    }

    public static TripleMessageProducer withSupplier(ThrowableSupplier<Object> supplier) {
        return new TripleMessageProducer(supplier);
    }
}
