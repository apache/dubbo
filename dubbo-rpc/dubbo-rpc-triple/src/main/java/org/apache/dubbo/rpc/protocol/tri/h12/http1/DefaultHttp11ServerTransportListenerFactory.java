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
package org.apache.dubbo.rpc.protocol.tri.h12.http1;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListener;
import org.apache.dubbo.remoting.http12.h1.Http1ServerTransportListenerFactory;
import org.apache.dubbo.rpc.model.FrameworkModel;

public class DefaultHttp11ServerTransportListenerFactory implements Http1ServerTransportListenerFactory {

    public static final Http1ServerTransportListenerFactory INSTANCE = new DefaultHttp11ServerTransportListenerFactory();

    @Override
    public Http1ServerTransportListener newInstance(HttpChannel httpChannel, URL url, FrameworkModel frameworkModel) {
        return new DefaultHttp11ServerTransportListener(httpChannel, url, frameworkModel);
    }
}
