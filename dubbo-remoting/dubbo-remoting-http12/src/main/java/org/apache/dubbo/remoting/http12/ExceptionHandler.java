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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.rpc.model.MethodDescriptor;

/**
 * Interface for customize exception handling.
 *
 * @param <E> the type of exception to handle
 * @param <T> the type of result returned
 */
@SPI(scope = ExtensionScope.FRAMEWORK)
public interface ExceptionHandler<E extends Throwable, T> {

    /**
     * Resolves the log level for a given throwable.
     *
     * @param throwable the exception
     * @return the log level, or null to ignore this extension
     */
    default Level resolveLogLevel(E throwable) {
        return null;
    }

    /**
     * Resolves the gRPC status for a given throwable.
     *
     * @param headers    the response headers
     * @param throwable  the exception
     * @param metadata   the request metadata, may be null
     * @param descriptor the method descriptor, may be null
     */
    default boolean resolveGrpcStatus(
            E throwable, HttpHeaders headers, RequestMetadata metadata, MethodDescriptor descriptor) {
        return false;
    }

    /**
     * Handle the exception and return a result.
     *
     * @param throwable  the exception
     * @param metadata   the request metadata, may be null
     * @param descriptor the method descriptor, may be null
     * @return a result of type T, or null to ignore this extension
     */
    default T handle(E throwable, RequestMetadata metadata, MethodDescriptor descriptor) {
        return null;
    }

    /**
     * Handles the exception and return a result for gRPC protocol.
     *
     * @param throwable  the exception
     * @param metadata   the request metadata, may be null
     * @param descriptor the method descriptor, may be null
     * @return a result of type T, or null to ignore this extension
     */
    default T handleGrpc(E throwable, RequestMetadata metadata, MethodDescriptor descriptor) {
        return null;
    }
}
