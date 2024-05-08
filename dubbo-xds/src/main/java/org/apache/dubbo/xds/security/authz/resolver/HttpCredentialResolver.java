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
package org.apache.dubbo.xds.security.authz.resolver;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.xds.security.authz.RequestCredential;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import java.util.List;
import java.util.Map;

@Activate
public class HttpCredentialResolver implements CredentialResolver {

    private static final String TRIPLE_NAME = "tri";

    private static final String REST_NAME = "rest";

    @Override
    public void appendRequestCredential(URL url, Invocation invocation, RequestCredential requestCredential) {
        if (!(TRIPLE_NAME.equals(url.getProtocol()) || REST_NAME.equals(url.getProtocol()))) {
            return;
        }
        String targetPath = invocation.getServiceName() + "/" + invocation.getMethodName();
        String httpMethod = "POST";
        requestCredential.add(RequestAuthProperty.URL_PATH, targetPath);
        requestCredential.add(RequestAuthProperty.HTTP_METHOD, httpMethod);

        // TODO get more detailed http message from context
        Map<String, String> requestHttpHeaders = null;
        requestCredential.add(RequestAuthProperty.JWT_FROM_HEADERS, requestHttpHeaders);

        Map<String, List<String>> httpRequestParams = null;
        requestCredential.add(RequestAuthProperty.JWT_FROM_PARAMS, httpRequestParams);
    }
}
