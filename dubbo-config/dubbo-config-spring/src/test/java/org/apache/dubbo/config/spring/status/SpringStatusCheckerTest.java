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
package org.apache.dubbo.config.spring.status;

import org.apache.dubbo.common.status.Status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.web.context.support.GenericWebApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class SpringStatusCheckerTest {

//    @Mock
//    private ApplicationLifeCycle applicationContext;

    @BeforeEach
    public void setUp() throws Exception {
        //initMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        //Mockito.reset(applicationContext);
    }

    @Test
    public void testWithoutApplicationContext() {
        SpringStatusChecker springStatusChecker = new SpringStatusChecker((ApplicationContext) null);
        Status status = springStatusChecker.check();

        assertThat(status.getLevel(), is(Status.Level.UNKNOWN));
    }

    @Test
    public void testWithLifeCycleRunning() {
        ApplicationLifeCycle applicationLifeCycle = mock(ApplicationLifeCycle.class);
        given(applicationLifeCycle.getConfigLocations()).willReturn(new String[]{"test1", "test2"});
        given(applicationLifeCycle.isRunning()).willReturn(true);

        SpringStatusChecker springStatusChecker = new SpringStatusChecker(applicationLifeCycle);
        Status status = springStatusChecker.check();

        assertThat(status.getLevel(), is(Status.Level.OK));
        assertThat(status.getMessage(), is("test1,test2"));
    }

    @Test
    public void testWithoutLifeCycleRunning() {
        ApplicationLifeCycle applicationLifeCycle = mock(ApplicationLifeCycle.class);
        given(applicationLifeCycle.isRunning()).willReturn(false);

        SpringStatusChecker springStatusChecker = new SpringStatusChecker(applicationLifeCycle);
        Status status = springStatusChecker.check();

        assertThat(status.getLevel(), is(Status.Level.ERROR));
    }

    interface ApplicationLifeCycle extends Lifecycle, ApplicationContext {
        String[] getConfigLocations();
    }

    // TODO improve GenericWebApplicationContext test scenario
    @Test
    public void testGenericWebApplicationContext() {
        GenericWebApplicationContext context = mock(GenericWebApplicationContext.class);
        given(context.isRunning()).willReturn(true);

        SpringStatusChecker checker = new SpringStatusChecker(context);
        Status status = checker.check();
        Assertions.assertEquals(Status.Level.OK, status.getLevel());
    }

}
