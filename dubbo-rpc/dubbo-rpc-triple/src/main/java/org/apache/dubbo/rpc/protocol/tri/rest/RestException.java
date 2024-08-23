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

import org.apache.dubbo.remoting.http12.exception.HttpStatusException;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;

public class RestException extends HttpStatusException {

    private static final long serialVersionUID = 1L;

    private final Messages message;
    private final String displayMessage;

    public RestException(String message) {
        super(500, message);
        this.message = Messages.INTERNAL_ERROR;
        displayMessage = null;
    }

    public RestException(Throwable cause) {
        super(500, ExceptionUtils.unwrap(cause));
        message = Messages.INTERNAL_ERROR;
        displayMessage = null;
    }

    public RestException(String message, Throwable cause) {
        super(500, message, ExceptionUtils.unwrap(cause));
        this.message = Messages.INTERNAL_ERROR;
        displayMessage = null;
    }

    public RestException(Messages message, Object... arguments) {
        super(message.statusCode(), message.format(arguments));
        this.message = message;
        displayMessage = message.formatDisplay(arguments);
    }

    public RestException(Throwable cause, Messages message, Object... arguments) {
        super(message.statusCode(), message.format(arguments), ExceptionUtils.unwrap(cause));
        this.message = message;
        displayMessage = message.formatDisplay(arguments);
    }

    public String getErrorCode() {
        return message.name();
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage == null ? getMessage() : displayMessage;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": status=" + getStatusCode() + ", " + getErrorCode() + ", " + getMessage();
    }
}
