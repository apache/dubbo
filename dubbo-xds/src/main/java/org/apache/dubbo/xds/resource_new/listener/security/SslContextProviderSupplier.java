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

import java.io.Closeable;
import java.util.Objects;

import io.netty.handler.ssl.SslContext;

import org.apache.dubbo.common.utils.Assert;

/**
 * Enables Client or server side to initialize this object with the received {@link BaseTlsContext} and communicate it
 * to the consumer i.e. {@link SecurityProtocolNegotiators} to lazily evaluate the {@link SslContextProvider}. The
 * supplier prevents credentials leakage in cases where the user is not using xDS credentials but the client/server
 * contains a non-default {@link BaseTlsContext}.
 */
public final class SslContextProviderSupplier implements Closeable {

    private final BaseTlsContext tlsContext;
    private final TlsContextManager tlsContextManager;
    private SslContextProvider sslContextProvider;
    private boolean shutdown;

    public SslContextProviderSupplier(
            BaseTlsContext tlsContext, TlsContextManager tlsContextManager) {
        Assert.notNull(tlsContext, "tlsContext must not be null");
        Assert.notNull(tlsContextManager, "tlsContextManager must not be null");
        this.tlsContext = tlsContext;
        this.tlsContextManager = tlsContextManager;
    }

    public BaseTlsContext getTlsContext() {
        return tlsContext;
    }

    /**
     * Updates SslContext via the passed callback.
     */
    public synchronized void updateSslContext(final SslContextProvider.Callback callback) {
        Assert.notNull(callback, "callback must not be null");
        try {
            if (!shutdown) {
                if (sslContextProvider == null) {
                    sslContextProvider = getSslContextProvider();
                }
            }
            // we want to increment the ref-count so call findOrCreate again...
            final SslContextProvider toRelease = getSslContextProvider();
            toRelease.addCallback(new SslContextProvider.Callback(callback.getExecutor()) {

                @Override
                public void updateSslContext(SslContext sslContext) {
                    callback.updateSslContext(sslContext);
                    releaseSslContextProvider(toRelease);
                }

                @Override
                public void onException(Throwable throwable) {
                    callback.onException(throwable);
                    releaseSslContextProvider(toRelease);
                }
            });
        } catch (final Throwable throwable) {
            callback.getExecutor()
                    .execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onException(throwable);
                        }
                    });
        }
    }

    private void releaseSslContextProvider(SslContextProvider toRelease) {
        if (tlsContext instanceof UpstreamTlsContext) {
            tlsContextManager.releaseClientSslContextProvider(toRelease);
        } else {
            tlsContextManager.releaseServerSslContextProvider(toRelease);
        }
    }

    private SslContextProvider getSslContextProvider() {
        return tlsContext instanceof UpstreamTlsContext ?
                tlsContextManager.findOrCreateClientSslContextProvider((UpstreamTlsContext) tlsContext) :
                tlsContextManager.findOrCreateServerSslContextProvider((DownstreamTlsContext) tlsContext);
    }

    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Called by consumer when tlsContext changes.
     */
    @Override
    public synchronized void close() {
        if (sslContextProvider != null) {
            if (tlsContext instanceof UpstreamTlsContext) {
                tlsContextManager.releaseClientSslContextProvider(sslContextProvider);
            } else {
                tlsContextManager.releaseServerSslContextProvider(sslContextProvider);
            }
        }
        sslContextProvider = null;
        shutdown = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SslContextProviderSupplier that = (SslContextProviderSupplier) o;
        return Objects.equals(tlsContext, that.tlsContext) && Objects.equals(tlsContextManager, that.tlsContextManager);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tlsContext, tlsContextManager);
    }

    @Override
    public String toString() {
        return "SslContextProviderSupplier{" + "tlsContext=" + tlsContext + ", tlsContextManager=" + tlsContextManager
                + ", sslContextProvider=" + sslContextProvider + ", shutdown=" + shutdown + '}';
    }
}
