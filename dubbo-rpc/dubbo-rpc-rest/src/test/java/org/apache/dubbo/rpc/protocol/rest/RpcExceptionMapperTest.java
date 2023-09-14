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

import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.internal.util.collections.Sets;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RpcExceptionMapperTest {

    private ExceptionHandler exceptionMapper;

    @BeforeEach
    public void setUp() {
        this.exceptionMapper = new RpcExceptionMapper();
    }

    @Test
    void testConstraintViolationException() {
        ConstraintViolationException violationException = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class, Answers.RETURNS_DEEP_STUBS);
        given(violationException.getConstraintViolations()).willReturn(Sets.newSet(violation));
        RpcException rpcException = new RpcException("violation", violationException);

        Object response = exceptionMapper.result(rpcException);

        assertThat(response, not(nullValue()));
        assertThat(response, instanceOf(ViolationReport.class));
    }

    @Test
    void testNormalException() {
        RpcException rpcException = new RpcException();
        Object response = exceptionMapper.result(rpcException);


        assertThat(response, not(nullValue()));
        assertThat(response, instanceOf(String.class));
    }

    @Test
    void testBuildException() {

        RestConstraintViolation restConstraintViolation = new RestConstraintViolation();
        String message = "message";
        restConstraintViolation.setMessage(message);
        String path = "path";
        restConstraintViolation.setPath(path);
        String value = "value";
        restConstraintViolation.setValue(value);


        Assertions.assertEquals(message, restConstraintViolation.getMessage());
        Assertions.assertEquals(path, restConstraintViolation.getPath());
        Assertions.assertEquals(value, restConstraintViolation.getValue());


    }

    @Test
    public void testViolationReport() {

        ViolationReport violationReport = new ViolationReport();


        RestConstraintViolation restConstraintViolation = new RestConstraintViolation("path", "message", "value");

        violationReport.addConstraintViolation(restConstraintViolation);

        Assertions.assertEquals(1, violationReport.getConstraintViolations().size());


        violationReport = new ViolationReport();

        violationReport.setConstraintViolations(new LinkedList<>());

        Assertions.assertEquals(0, violationReport.getConstraintViolations().size());


    }
}
