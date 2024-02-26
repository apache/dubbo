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

import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionHandler;
import org.apache.dubbo.rpc.protocol.rest.util.ConstraintViolationExceptionConvert;

public class RpcExceptionMapper implements ExceptionHandler<RpcException> {

    @Override
    public Object result(RpcException e) {

        // javax dependency judge
        if (violationDependency()) {
            //  ConstraintViolationException judge
            if (ConstraintViolationExceptionConvert.needConvert(e)) {
                return ConstraintViolationExceptionConvert.handleConstraintViolationException(e);
            }
        }

        return "Internal server error: " + e.getMessage();
    }

    private boolean violationDependency() {
        return ClassUtils.isPresent(
                "javax.validation.ConstraintViolationException", RpcExceptionMapper.class.getClassLoader());
    }
}
