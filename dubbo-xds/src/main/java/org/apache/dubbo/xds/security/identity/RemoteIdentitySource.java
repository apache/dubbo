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
package org.apache.dubbo.xds.security.identity;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.xds.security.api.ServiceIdentitySource;

import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RemoteIdentitySource implements ServiceIdentitySource {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(RemoteIdentitySource.class);

    private static final String REMOTE_IDENTITY_KEY = "remoteIdentity";

    private final OkHttpClient httpClient;

    public RemoteIdentitySource() {
        this.httpClient = new OkHttpClient.Builder().build();
    }

    @Override
    public String getToken(URL url) {
        String tokenServiceAddr = url.getParameter(REMOTE_IDENTITY_KEY);
        try (Response response = httpClient
                .newCall(new Builder().get().url(tokenServiceAddr).build())
                .execute()) {
            ResponseBody body = response.body();
            return body == null ? null : body.string();
        } catch (Exception e) {
            logger.error("99-1", "", "", "Failed to get token from remote service", e);
        }
        return null;
    }
}
