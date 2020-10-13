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
package org.apache.dubbo.common.url.component;

import org.apache.dubbo.common.URL;

import java.util.Map;

public class HSFServiceAddressURL extends ServiceAddressURL {

    public HSFServiceAddressURL(String protocol, String username, String password, String host, int port, String path, Map<String, String> parameters, URL consumerURL) {
        super(protocol, username, password, host, port, path, parameters, consumerURL);
    }

    public HSFServiceAddressURL(URLAddress urlAddress, URLParam urlParam, URL consumerURL) {
        super(urlAddress, urlParam, consumerURL);
    }

    protected <T extends URL> T newURL(URLAddress urlAddress, URLParam urlParam) {
        return (T) new HSFServiceAddressURL(urlAddress, urlParam, this.consumerURL);
    }

    @Override
    public String getPath() {
        return getConsumerURL().getPath();
    }

    @Override
    public String getProtocol() {
        return "HSF";
    }
}
