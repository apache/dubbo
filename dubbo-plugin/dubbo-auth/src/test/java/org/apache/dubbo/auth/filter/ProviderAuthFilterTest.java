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
package org.apache.dubbo.auth.filter;

import org.apache.dubbo.auth.Constants;
import org.apache.dubbo.auth.exception.RpcAuthenticationException;
import org.apache.dubbo.auth.utils.SignatureUtils;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class ProviderAuthFilterTest {
    @Test
    void testAuthDisabled() {
        URL url = mock(URL.class);
        Invoker invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        when(invoker.getUrl()).thenReturn(url);
        ProviderAuthFilter providerAuthFilter = new ProviderAuthFilter();
        providerAuthFilter.invoke(invoker, invocation);
        verify(url, never()).getParameter(eq(Constants.AUTHENTICATOR), eq(Constants.DEFAULT_AUTHENTICATOR));
    }

    @Test
    void testAuthEnabled() {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk")
                .addParameter(CommonConstants.APPLICATION_KEY, "test")
                .addParameter(Constants.SERVICE_AUTH, true);
        Invoker invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        when(invoker.getUrl()).thenReturn(url);
        ProviderAuthFilter providerAuthFilter = new ProviderAuthFilter();
        providerAuthFilter.invoke(invoker, invocation);
        verify(invocation, atLeastOnce()).getAttachment(anyString());
    }


    @Test
    void testAuthFailed() {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk")
                .addParameter(CommonConstants.APPLICATION_KEY, "test")
                .addParameter(Constants.SERVICE_AUTH, true);
        Invoker invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getAttachment(Constants.REQUEST_SIGNATURE_KEY)).thenReturn(null);
        when(invoker.getUrl()).thenReturn(url);

        ProviderAuthFilter providerAuthFilter = new ProviderAuthFilter();
        Result result = providerAuthFilter.invoke(invoker, invocation);
        assertTrue(result.hasException());

    }

    @Test
    void testAuthFailedWhenNoSignature() {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk")
                .addParameter(CommonConstants.APPLICATION_KEY, "test")
                .addParameter(Constants.SERVICE_AUTH, true);
        Invoker invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getAttachment(Constants.REQUEST_SIGNATURE_KEY)).thenReturn(null);
        when(invoker.getUrl()).thenReturn(url);

        ProviderAuthFilter providerAuthFilter = new ProviderAuthFilter();
        Result result = providerAuthFilter.invoke(invoker, invocation);
        assertTrue(result.hasException());
    }

    @Test
    void testAuthFailedWhenNoAccessKeyPair() {
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .addParameter(CommonConstants.APPLICATION_KEY, "test-provider")
                .addParameter(Constants.SERVICE_AUTH, true);
        Invoker invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getObjectAttachment(Constants.REQUEST_SIGNATURE_KEY)).thenReturn("dubbo");
        when(invocation.getObjectAttachment(Constants.AK_KEY)).thenReturn("ak");
        when(invocation.getObjectAttachment(CommonConstants.CONSUMER)).thenReturn("test-consumer");
        when(invocation.getObjectAttachment(Constants.REQUEST_TIMESTAMP_KEY)).thenReturn(System.currentTimeMillis());
        when(invoker.getUrl()).thenReturn(url);

        ProviderAuthFilter providerAuthFilter = new ProviderAuthFilter();
        Result result = providerAuthFilter.invoke(invoker, invocation);
        assertTrue(result.hasException());
        assertTrue(result.getException() instanceof RpcAuthenticationException);
    }

    @Test
    void testAuthFailedWhenParameterError() {
        String service = "org.apache.dubbo.DemoService";
        String method = "test";
        Object[] originalParams = new Object[]{"dubbo1", "dubbo2"};
        long currentTimeMillis = System.currentTimeMillis();
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .setServiceInterface(service)
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk")
                .addParameter(CommonConstants.APPLICATION_KEY, "test-provider")
                .addParameter(Constants.PARAMETER_SIGNATURE_ENABLE_KEY, true)
                .addParameter(Constants.SERVICE_AUTH, true);

        Invoker invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getObjectAttachment(Constants.AK_KEY)).thenReturn("ak");
        when(invocation.getObjectAttachment(CommonConstants.CONSUMER)).thenReturn("test-consumer");
        when(invocation.getObjectAttachment(Constants.REQUEST_TIMESTAMP_KEY)).thenReturn(currentTimeMillis);
        when(invocation.getMethodName()).thenReturn(method);
        Object[] fakeParams = new Object[]{"dubbo1", "dubbo3"};
        when(invocation.getArguments()).thenReturn(fakeParams);
        when(invoker.getUrl()).thenReturn(url);


        String requestString = String.format(Constants.SIGNATURE_STRING_FORMAT,
                url.getColonSeparatedKey(), invocation.getMethodName(), "sk", currentTimeMillis);
        String sign = SignatureUtils.sign(originalParams, requestString, "sk");
        when(invocation.getObjectAttachment(Constants.REQUEST_SIGNATURE_KEY)).thenReturn(sign);

        ProviderAuthFilter providerAuthFilter = new ProviderAuthFilter();
        Result result = providerAuthFilter.invoke(invoker, invocation);
        assertTrue(result.hasException());
        assertTrue(result.getException() instanceof RpcAuthenticationException);
    }

    @Test
    void testAuthSuccessfully() {
        String service = "org.apache.dubbo.DemoService";
        String method = "test";
        long currentTimeMillis = System.currentTimeMillis();
        URL url = URL.valueOf("dubbo://10.10.10.10:2181")
                .setServiceInterface(service)
                .addParameter(Constants.ACCESS_KEY_ID_KEY, "ak")
                .addParameter(Constants.SECRET_ACCESS_KEY_KEY, "sk")
                .addParameter(CommonConstants.APPLICATION_KEY, "test-provider")
                .addParameter(Constants.SERVICE_AUTH, true);
        Invoker invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        when(invocation.getAttachment(Constants.AK_KEY)).thenReturn("ak");
        when(invocation.getAttachment(CommonConstants.CONSUMER)).thenReturn("test-consumer");
        when(invocation.getAttachment(Constants.REQUEST_TIMESTAMP_KEY)).thenReturn(String.valueOf(currentTimeMillis));
        when(invocation.getMethodName()).thenReturn(method);
        when(invoker.getUrl()).thenReturn(url);


        String requestString = String.format(Constants.SIGNATURE_STRING_FORMAT,
                url.getColonSeparatedKey(), invocation.getMethodName(), "sk", currentTimeMillis);
        String sign = SignatureUtils.sign(requestString, "sk");
        when(invocation.getAttachment(Constants.REQUEST_SIGNATURE_KEY)).thenReturn(sign);

        ProviderAuthFilter providerAuthFilter = new ProviderAuthFilter();
        Result result = providerAuthFilter.invoke(invoker, invocation);
        assertNull(result);
    }
}