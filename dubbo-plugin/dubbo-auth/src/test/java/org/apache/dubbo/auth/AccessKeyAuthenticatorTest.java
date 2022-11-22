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
import org.apache.dubbo.auth.model.AccessKeyPair;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class AccessKeyAuthenticatorTest {

    @Test
    void testSignForRequest() {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(CommonConstants.APPLICATION_KEY, "test")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk");
        Invocation invocation = new RpcInvocation();

        AccessKeyAuthenticator helper = mock(AccessKeyAuthenticator.class);
        doCallRealMethod().when(helper).sign(invocation, url);
        when(helper.getSignature(eq(url), eq(invocation), eq("sk"), anyString())).thenReturn("dubbo");

        AccessKeyPair accessKeyPair = mock(AccessKeyPair.class);
        when(accessKeyPair.getSecretKey()).thenReturn("sk");
        when(helper.getAccessKeyPair(invocation, url)).thenReturn(accessKeyPair);

        helper.sign(invocation, url);
        assertEquals(String.valueOf(invocation.getAttachment(CommonConstants.CONSUMER)), url.getApplication());
        assertNotNull(invocation.getAttachments().get(Constants.REQUEST_SIGNATURE_KEY));
        assertEquals(invocation.getAttachments().get(Constants.REQUEST_SIGNATURE_KEY), "dubbo");
    }

    @Test
    void testAuthenticateRequest() throws RpcAuthenticationException {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(CommonConstants.APPLICATION_KEY, "test")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk");
        Invocation invocation = new RpcInvocation();
        invocation.setAttachment(Constants.ACCESS_KEY_ID_KEY, "ak");
        invocation.setAttachment(Constants.REQUEST_SIGNATURE_KEY, "dubbo");
        invocation.setAttachment(Constants.REQUEST_TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
        invocation.setAttachment(CommonConstants.CONSUMER, "test");

        AccessKeyAuthenticator helper = mock(AccessKeyAuthenticator.class);
        doCallRealMethod().when(helper).authenticate(invocation, url);
        when(helper.getSignature(eq(url), eq(invocation), eq("sk"), anyString())).thenReturn("dubbo");

        AccessKeyPair accessKeyPair = mock(AccessKeyPair.class);
        when(accessKeyPair.getSecretKey()).thenReturn("sk");
        when(helper.getAccessKeyPair(invocation, url)).thenReturn(accessKeyPair);

        assertDoesNotThrow(() -> helper.authenticate(invocation, url));
    }

    @Test
    void testAuthenticateRequestNoSignature() {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(CommonConstants.APPLICATION_KEY, "test")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk");
        Invocation invocation = new RpcInvocation();
        AccessKeyAuthenticator helper = new AccessKeyAuthenticator(ApplicationModel.defaultModel());
        assertThrows(RpcAuthenticationException.class, () -> helper.authenticate(invocation, url));
    }

    @Test
    void testGetAccessKeyPairFailed() {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak");
        AccessKeyAuthenticator helper = new AccessKeyAuthenticator(ApplicationModel.defaultModel());
        Invocation invocation = mock(Invocation.class);
        assertThrows(RuntimeException.class, () -> helper.getAccessKeyPair(invocation, url));
    }

    @Test
    void testGetSignatureNoParameter() {
        URL url = mock(URL.class);
        Invocation invocation = mock(Invocation.class);
        String secretKey = "123456";
        AccessKeyAuthenticator helper = new AccessKeyAuthenticator(ApplicationModel.defaultModel());
        String signature = helper.getSignature(url, invocation, secretKey, String.valueOf(System.currentTimeMillis()));
        assertNotNull(signature);
    }

    @Test
    void testGetSignatureWithParameter() {
        URL url = mock(URL.class);
        when(url.getParameter(Constants.PARAMETER_SIGNATURE_ENABLE_KEY, false)).thenReturn(true);
        Invocation invocation = mock(Invocation.class);
        String secretKey = "123456";
        Object[] params = {"dubbo", new ArrayList()};
        when(invocation.getArguments()).thenReturn(params);
        AccessKeyAuthenticator helper = new AccessKeyAuthenticator(ApplicationModel.defaultModel());
        String signature = helper.getSignature(url, invocation, secretKey, String.valueOf(System.currentTimeMillis()));
        assertNotNull(signature);

        Object[] fakeParams = {"dubbo1", new ArrayList<>()};
        when(invocation.getArguments()).thenReturn(fakeParams);
        String signature1 = helper.getSignature(url, invocation, secretKey, String.valueOf(System.currentTimeMillis()));
        assertNotEquals(signature, signature1);
    }
}