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

package org.apache.dubbo.rpc.service;

import org.apache.dubbo.common.utils.JsonUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class GenericExceptionTest {

    @Test
    void jsonSupport() throws IOException {
        {
            GenericException src = new GenericException();
            String s = JsonUtils.toJson(src);
            GenericException dst = JsonUtils.toJavaObject(s, GenericException.class);
            Assertions.assertEquals(src.getExceptionClass(), dst.getExceptionClass());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
            Assertions.assertEquals(src.getMessage(), dst.getMessage());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
        }
        {
            GenericException src = new GenericException(this.getClass().getSimpleName(), "test");
            String s = JsonUtils.toJson(src);
            GenericException dst = JsonUtils.toJavaObject(s, GenericException.class);
            Assertions.assertEquals(src.getExceptionClass(), dst.getExceptionClass());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
            Assertions.assertEquals(src.getMessage(), dst.getMessage());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
        }
        {
            Throwable throwable = new Throwable("throwable");
            GenericException src = new GenericException(throwable);
            String s = JsonUtils.toJson(src);
            GenericException dst = JsonUtils.toJavaObject(s, GenericException.class);
            Assertions.assertEquals(src.getExceptionClass(), dst.getExceptionClass());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
            Assertions.assertEquals(src.getMessage(), dst.getMessage());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
        }
    }
}
