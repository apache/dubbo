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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.common.ssl.ProviderCert;
import org.apache.dubbo.xds.registry.XdsEndpoint;

@Activate(order = -10000)
public final class NativeCertProvider implements CertProvider {
    @Override
    public boolean isSupport(URL url) {
        XdsEndpoint endpoint = NativeTlsAdapter.endpoint(url);
        return (endpoint != null && endpoint.getSecurity() != null)
                || XdsServerBindings.isManaged(url.getParameter("bind.port", url.getPort()));
    }

    @Override
    public Cert getConsumerConnectionConfig(URL url) {
        XdsEndpoint endpoint = NativeTlsAdapter.endpoint(url);
        return endpoint == null || endpoint.getSecurity() == null ? null : endpoint.getCertificate();
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL url) {
        XdsServerBindings.Binding binding = XdsServerBindings.binding(url.getParameter("bind.port", url.getPort()));
        return binding == null ? null : binding.credentials();
    }
}
