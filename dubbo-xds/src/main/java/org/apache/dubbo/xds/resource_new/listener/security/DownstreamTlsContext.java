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
package org.apache.dubbo.xds.resource_new.listener.security;

import java.util.Objects;

import io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.CommonTlsContext;

public class DownstreamTlsContext extends BaseTlsContext {

    private final boolean requireClientCertificate;

    public DownstreamTlsContext(CommonTlsContext commonTlsContext, boolean requireClientCertificate) {
        super(commonTlsContext);
        this.requireClientCertificate = requireClientCertificate;
    }

    public static DownstreamTlsContext fromEnvoyProtoDownstreamTlsContext(
            io.envoyproxy.envoy.extensions.transport_sockets.tls.v3.DownstreamTlsContext downstreamTlsContext) {
        return new DownstreamTlsContext(
                downstreamTlsContext.getCommonTlsContext(), downstreamTlsContext.hasRequireClientCertificate());
    }

    public boolean isRequireClientCertificate() {
        return requireClientCertificate;
    }

    @Override
    public String toString() {
        return "DownstreamTlsContext{" + "commonTlsContext=" + commonTlsContext + ", requireClientCertificate="
                + requireClientCertificate + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DownstreamTlsContext that = (DownstreamTlsContext) o;
        return requireClientCertificate == that.requireClientCertificate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), requireClientCertificate);
    }
}
