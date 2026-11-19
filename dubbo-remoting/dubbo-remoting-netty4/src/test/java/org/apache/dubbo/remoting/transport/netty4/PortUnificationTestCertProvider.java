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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.ssl.AuthPolicy;
import org.apache.dubbo.common.ssl.Cert;
import org.apache.dubbo.common.ssl.CertProvider;
import org.apache.dubbo.common.ssl.ProviderCert;

@Activate(order = -10000)
public class PortUnificationTestCertProvider implements CertProvider {

    @Override
    public boolean isSupport(URL address) {
        return address.getParameter("pu.test.cert", false);
    }

    @Override
    public ProviderCert getProviderConnectionConfig(URL localAddress) {
        return new ProviderCert(new byte[0], new byte[0], null, AuthPolicy.NONE);
    }

    @Override
    public Cert getConsumerConnectionConfig(URL remoteAddress) {
        return null;
    }
}
