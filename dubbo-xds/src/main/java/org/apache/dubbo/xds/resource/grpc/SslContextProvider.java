/*
 * Copyright 2019 The gRPC Authors
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

package org.apache.dubbo.xds.resource.grpc;

import org.apache.dubbo.xds.resource.grpc.EnvoyServerProtoData.BaseTlsContext;
import org.apache.dubbo.xds.resource.grpc.EnvoyServerProtoData.DownstreamTlsContext;
import org.apache.dubbo.xds.resource.grpc.EnvoyServerProtoData.UpstreamTlsContext;

import com.google.common.annotations.VisibleForTesting;
import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;
import io.grpc.Internal;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A SslContextProvider is a "container" or provider of SslContext. This is used by gRPC-xds to
 * obtain an SslContext, so is not part of the public API of gRPC. This "container" may represent a
 * stream that is receiving the requested secret(s) or it could represent file-system based
 * secret(s) that are dynamic.
 */
@Internal
public abstract class SslContextProvider implements Closeable {

  protected final BaseTlsContext tlsContext;

  @VisibleForTesting public abstract static class Callback {
    private final Executor executor;

    protected Callback(Executor executor) {
      this.executor = executor;
    }

    @VisibleForTesting public Executor getExecutor() {
      return executor;
    }

    /** Informs callee of new/updated SslContext. */
    @VisibleForTesting public abstract void updateSslContext(SslContext sslContext);

    /** Informs callee of an exception that was generated. */
    @VisibleForTesting protected abstract void onException(Throwable throwable);
  }

  protected SslContextProvider(BaseTlsContext tlsContext) {
    this.tlsContext = checkNotNull(tlsContext, "tlsContext");
  }

  protected CommonTlsContext getCommonTlsContext() {
    return tlsContext.getCommonTlsContext();
  }

  protected void setClientAuthValues(
      SslContextBuilder sslContextBuilder, XdsTrustManagerFactory xdsTrustManagerFactory)
      throws CertificateException, IOException, CertStoreException {
    DownstreamTlsContext downstreamTlsContext = getDownstreamTlsContext();
    if (xdsTrustManagerFactory != null) {
      sslContextBuilder.trustManager(xdsTrustManagerFactory);
      sslContextBuilder.clientAuth(
          downstreamTlsContext.isRequireClientCertificate()
              ? ClientAuth.REQUIRE
              : ClientAuth.OPTIONAL);
    } else {
      sslContextBuilder.clientAuth(ClientAuth.NONE);
    }
  }

  /** Returns the DownstreamTlsContext in this SslContextProvider if this is server side. **/
  public DownstreamTlsContext getDownstreamTlsContext() {
    checkState(tlsContext instanceof DownstreamTlsContext,
        "expected DownstreamTlsContext");
    return ((DownstreamTlsContext)tlsContext);
  }

  /** Returns the UpstreamTlsContext in this SslContextProvider if this is client side. **/
  public UpstreamTlsContext getUpstreamTlsContext() {
    checkState(tlsContext instanceof UpstreamTlsContext,
        "expected UpstreamTlsContext");
    return ((UpstreamTlsContext)tlsContext);
  }

  /** Closes this provider and releases any resources. */
  @Override
  public abstract void close();

  /**
   * Registers a callback on the given executor. The callback will run when SslContext becomes
   * available or immediately if the result is already available.
   */
  public abstract void addCallback(Callback callback);

  protected final void performCallback(
          final SslContextGetter sslContextGetter, final Callback callback) {
    checkNotNull(sslContextGetter, "sslContextGetter");
    checkNotNull(callback, "callback");
    callback.executor.execute(
        new Runnable() {
          @Override
          public void run() {
            try {
              SslContext sslContext = sslContextGetter.get();
              callback.updateSslContext(sslContext);
            } catch (Throwable e) {
              callback.onException(e);
            }
          }
        });
  }

  /** Allows implementations to compute or get SslContext. */
  protected interface SslContextGetter {
    SslContext get() throws Exception;
  }
}
