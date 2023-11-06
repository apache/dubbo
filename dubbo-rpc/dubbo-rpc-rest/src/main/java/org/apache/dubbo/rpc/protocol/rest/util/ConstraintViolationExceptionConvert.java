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
package org.apache.dubbo.rpc.protocol.rest.util;

import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.rest.RestConstraintViolation;
import org.apache.dubbo.rpc.protocol.rest.ViolationReport;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

public class ConstraintViolationExceptionConvert {

    public static Object handleConstraintViolationException(RpcException rpcException) {
        ConstraintViolationException cve = (ConstraintViolationException) rpcException.getCause();
        ViolationReport report = new ViolationReport();
        for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
            report.addConstraintViolation(new RestConstraintViolation(
                    cv.getPropertyPath().toString(),
                    cv.getMessage(),
                    cv.getInvalidValue() == null ? "null" : cv.getInvalidValue().toString()));
        }
        return report;
    }

    public static boolean needConvert(RpcException e) {
        return isConstraintViolationException(e);
    }

    private static boolean isConstraintViolationException(RpcException e) {
        try {
            return e.getCause() instanceof ConstraintViolationException;
        } catch (Throwable throwable) {
            return false;
        }
    }
}
