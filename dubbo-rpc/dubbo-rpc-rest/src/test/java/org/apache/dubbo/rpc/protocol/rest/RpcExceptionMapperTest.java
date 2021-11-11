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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.internal.util.collections.Sets;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class RpcExceptionMapperTest {

    private RpcExceptionMapper exceptionMapper;

    @BeforeEach
    public void setUp() {
        this.exceptionMapper = new RpcExceptionMapper();
    }

    @Test
    public void testConstraintViolationException() {
        ConstraintViolationException violationException = mock(ConstraintViolationException.class);
        ConstraintViolation violation = mock(ConstraintViolation.class, Answers.RETURNS_DEEP_STUBS);
        given(violationException.getConstraintViolations()).willReturn(Sets.<ConstraintViolation<?>>newSet(violation));
        RpcException rpcException = new RpcException("violation", violationException);

        Response response = exceptionMapper.toResponse(rpcException);

        assertThat(response, not(nullValue()));
        assertThat(response.getEntity(), instanceOf(ViolationReport.class));
    }

    @Test
    public void testNormalException() {
        RpcException rpcException = new RpcException();
        Response response = exceptionMapper.toResponse(rpcException);


        assertThat(response, not(nullValue()));
        assertThat(response.getEntity(), instanceOf(String.class));
    }
}