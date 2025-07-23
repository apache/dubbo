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
package org.apache.dubbo.rpc.protocol.tri.rest;

public class RestBadRequestException extends RestException {

    private static final long serialVersionUID = 1L;

    public RestBadRequestException(String message) {
        super(Messages.BAD_REQUEST, message);
    }

    public RestBadRequestException(Throwable cause) {
        super(cause, Messages.BAD_REQUEST);
    }

    public RestBadRequestException(String message, Throwable cause) {
        super(cause, Messages.BAD_REQUEST, message);
    }

    public RestBadRequestException(Messages message, Object... arguments) {
        super(message, arguments);
    }

    public RestBadRequestException(Throwable cause, Messages message, Object... arguments) {
        super(cause, message, arguments);
    }
}
