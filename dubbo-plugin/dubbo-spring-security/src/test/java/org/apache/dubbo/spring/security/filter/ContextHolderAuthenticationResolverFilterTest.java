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
package org.apache.dubbo.spring.security.filter;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.spring.security.jackson.ObjectMapperCodec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContextHolderAuthenticationResolverFilterTest {

    @AfterEach
    public void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSecurityContextIsClearedAfterInvoke() {
        ApplicationModel applicationModel = mock(ApplicationModel.class);
        ScopeBeanFactory beanFactory = mock(ScopeBeanFactory.class);
        ObjectMapperCodec codec = mock(ObjectMapperCodec.class);

        when(applicationModel.getBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.getBean(ObjectMapperCodec.class)).thenReturn(codec);

        ContextHolderAuthenticationResolverFilter filter =
                new ContextHolderAuthenticationResolverFilter(applicationModel);

        Invocation invocation = mock(Invocation.class);
        Invoker<?> invoker = mock(Invoker.class);

        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));
        filter.invoke(invoker, invocation);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        assertNull(
                auth, "SecurityContext must be cleared after the filter chain completes to prevent thread pollution.");
    }
}
