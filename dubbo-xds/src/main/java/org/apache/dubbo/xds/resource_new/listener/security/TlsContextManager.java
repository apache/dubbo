/*
 * Copyright 2020 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource_new.listener.security;

public interface TlsContextManager {

    /**
     * Creates a SslContextProvider. Used for retrieving a server-side SslContext.
     */
    SslContextProvider findOrCreateServerSslContextProvider(
            DownstreamTlsContext downstreamTlsContext);

    /**
     * Creates a SslContextProvider. Used for retrieving a client-side SslContext.
     */
    SslContextProvider findOrCreateClientSslContextProvider(
            UpstreamTlsContext upstreamTlsContext);

    /**
     * Releases an instance of the given client-side {@link SslContextProvider}.
     *
     * <p>The instance must have been obtained from {@link #findOrCreateClientSslContextProvider}.
     * Otherwise will throw IllegalArgumentException.
     *
     * <p>Caller must not release a reference more than once. It's advised that you clear the
     * reference to the instance with the null returned by this method.
     */
    SslContextProvider releaseClientSslContextProvider(SslContextProvider sslContextProvider);

    /**
     * Releases an instance of the given server-side {@link SslContextProvider}.
     *
     * <p>The instance must have been obtained from {@link #findOrCreateServerSslContextProvider}.
     * Otherwise will throw IllegalArgumentException.
     *
     * <p>Caller must not release a reference more than once. It's advised that you clear the
     * reference to the instance with the null returned by this method.
     */
    SslContextProvider releaseServerSslContextProvider(SslContextProvider sslContextProvider);
}
