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
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class ConsumerSignFilterTest {

    @Test
    void testAuthDisabled() {
        URL url = mock(URL.class);
        Invoker invoker = mock(Invoker.class);
        Invocation invocation = mock(Invocation.class);
        when(invoker.getUrl()).thenReturn(url);
        ConsumerSignFilter consumerSignFilter = new ConsumerSignFilter();
        consumerSignFilter.invoke(invoker, invocation);
        verify(invocation, never()).setAttachment(eq(Constants.REQUEST_SIGNATURE_KEY), anyString());
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
        ConsumerSignFilter consumerSignFilter = new ConsumerSignFilter();
        consumerSignFilter.invoke(invoker, invocation);
        verify(invocation, times(1)).setAttachment(eq(Constants.REQUEST_SIGNATURE_KEY), anyString());
    }
}