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
package org.apache.dubbo.auth;

import org.apache.dubbo.auth.exception.RpcAuthenticationException;
import org.apache.dubbo.auth.spi.Authenticator;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class BasicAuthenticator implements Authenticator {

    @Override
    public void sign(Invocation invocation, URL url) {
        String username = url.getParameter(Constants.USERNAME_KEY);
        String password = url.getParameter(Constants.PASSWORD_KEY);
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + encodedAuth;

        invocation.setAttachment(Constants.AUTHORIZATION_HEADER_LOWER, authHeaderValue);
    }

    @Override
    public void authenticate(Invocation invocation, URL url) throws RpcAuthenticationException {
        String username = url.getParameter(Constants.USERNAME_KEY);
        String password = url.getParameter(Constants.PASSWORD_KEY);
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + encodedAuth;

        if (!Objects.equals(authHeaderValue, invocation.getAttachment(Constants.AUTHORIZATION_HEADER))
                && !Objects.equals(authHeaderValue, invocation.getAttachment(Constants.AUTHORIZATION_HEADER_LOWER))) {
            throw new RpcAuthenticationException("Failed to authenticate, maybe consumer side did not enable the auth");
        }
    }
}
