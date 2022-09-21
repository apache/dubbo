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
package org.apache.dubbo.common.concurrent;

import org.apache.dubbo.common.threadpool.MemorySafeLinkedBlockingQueue;

/**
 * Exception thrown by an {@link MemorySafeLinkedBlockingQueue} when an element cannot be accepted.
 */
public class RejectException extends RuntimeException {

    private static final long serialVersionUID = -3240015871717170195L;

    /**
     * Constructs a {@code RejectException} with no detail message. The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause(Throwable) initCause}.
     */
    public RejectException() {
    }

    /**
     * Constructs a {@code RejectException} with the specified detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param message the detail message
     */
    public RejectException(final String message) {
        super(message);
    }

    /**
     * Constructs a {@code RejectException} with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public RejectException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code RejectException} with the specified cause.  The detail message is set to {@code (cause == null ? null :
     * cause.toString())} (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public RejectException(final Throwable cause) {
        super(cause);
    }
}
