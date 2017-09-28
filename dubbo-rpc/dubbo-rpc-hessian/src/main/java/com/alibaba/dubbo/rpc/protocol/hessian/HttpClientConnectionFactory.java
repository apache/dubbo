/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.hessian;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianConnectionFactory;
import com.caucho.hessian.client.HessianProxyFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import java.io.IOException;
import java.net.URL;

/**
 * HttpClientConnectionFactory
 *
 * @author william.liangf
 */
public class HttpClientConnectionFactory implements HessianConnectionFactory {

    private final HttpClient httpClient = new DefaultHttpClient();

    public void setHessianProxyFactory(HessianProxyFactory factory) {
        HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), (int) factory.getConnectTimeout());
        HttpConnectionParams.setSoTimeout(httpClient.getParams(), (int) factory.getReadTimeout());
    }

    public HessianConnection open(URL url) throws IOException {
        return new HttpClientConnection(httpClient, url);
    }

}