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
package org.apache.dubbo.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.spy;

public class AbstractMethodConfigTest {
    @Test
    public void testTimeout() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setTimeout(10);
        assertThat(methodConfig.getTimeout(), equalTo(10));
    }

    @Test
    public void testForks() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setForks(10);
        assertThat(methodConfig.getForks(), equalTo(10));
    }

    @Test
    public void testRetries() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setRetries(3);
        assertThat(methodConfig.getRetries(), equalTo(3));
    }

    @Test
    public void testLoadbalance() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setLoadbalance("mockloadbalance");
        assertThat(methodConfig.getLoadbalance(), equalTo("mockloadbalance"));
    }

    @Test
    public void testAsync() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setAsync(true);
        assertThat(methodConfig.isAsync(), is(true));
    }

    @Test
    public void testActives() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setActives(10);
        assertThat(methodConfig.getActives(), equalTo(10));
    }

    @Test
    public void testSent() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setSent(true);
        assertThat(methodConfig.getSent(), is(true));
    }

    @Test
    public void testMock() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setMock((Boolean) null);
        assertThat(methodConfig.getMock(), isEmptyOrNullString());
        methodConfig.setMock(true);
        assertThat(methodConfig.getMock(), equalTo("true"));
        methodConfig.setMock("return null");
        assertThat(methodConfig.getMock(), equalTo("return null"));
        methodConfig.setMock("mock");
        assertThat(methodConfig.getMock(), equalTo("mock"));
    }

    @Test
    public void testMerger() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setMerger("merger");
        assertThat(methodConfig.getMerger(), equalTo("merger"));
    }

    @Test
    public void testCache() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setCache("cache");
        assertThat(methodConfig.getCache(), equalTo("cache"));
    }

    @Test
    public void testValidation() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        methodConfig.setValidation("validation");
        assertThat(methodConfig.getValidation(), equalTo("validation"));
    }

    @Test
    public void testParameters() throws Exception {
        // Construct mock object
        AbstractMethodConfig methodConfig = spy(AbstractMethodConfig.class);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("key", "value");
        methodConfig.setParameters(parameters);
        assertThat(methodConfig.getParameters(), sameInstance(parameters));
    }
}
