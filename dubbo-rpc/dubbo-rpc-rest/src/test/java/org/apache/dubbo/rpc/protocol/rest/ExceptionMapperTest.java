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

import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionHandler;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ExceptionMapperTest {
    private final ExceptionMapper exceptionMapper = new ExceptionMapper();

    @Test
    void testRegister() {


        exceptionMapper.registerMapper(TestExceptionHandler.class);


        Object result = exceptionMapper.exceptionToResult(new RuntimeException("test"));


        Assertions.assertEquals("test", result);


    }

    @Test
    void testExceptionNoArgConstruct() {


        Assertions.assertThrows(RuntimeException.class, () -> {
            exceptionMapper.registerMapper(TestExceptionHandlerException.class);

        });


    }


    public class TestExceptionHandler implements ExceptionHandler<RuntimeException> {


        @Override
        public Object result(RuntimeException exception) {
            return exception.getMessage();
        }
    }

    class TestExceptionHandlerException implements ExceptionHandler<RuntimeException> {


        @Override
        public Object result(RuntimeException exception) {
            return exception.getMessage();
        }
    }


}
