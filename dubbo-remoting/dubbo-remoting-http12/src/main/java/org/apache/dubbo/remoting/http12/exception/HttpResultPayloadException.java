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
package org.apache.dubbo.remoting.http12.exception;

import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;

public class HttpResultPayloadException extends HttpStatusException {

    private static final long serialVersionUID = 1L;

    private final HttpResult<?> result;

    public HttpResultPayloadException(HttpResult<?> result) {
        super(result.getStatus());
        this.result = result;
    }

    public HttpResultPayloadException(int statusCode, Object body) {
        super(statusCode);
        result = HttpResult.of(statusCode, body);
    }

    public HttpResultPayloadException(Object body) {
        this(HttpStatus.OK.getCode(), body);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public HttpResult<?> getResult() {
        return result;
    }
}
