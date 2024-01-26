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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class RestException extends HttpStatusException {

    private static final long serialVersionUID = 1L;

    private final Messages message;

    public RestException(Messages message, Object... arguments) {
        super(message.statusCode(), message.format(arguments));
        this.message = message;
    }

    public RestException(Throwable cause, Messages message, Object... arguments) {
        super(message.statusCode(), message.format(arguments), unwrap(cause));
        this.message = message;
    }

    public RestException(String message, Throwable cause) {
        super(500, message, unwrap(cause));
        this.message = Messages.INTERNAL_ERROR;
    }

    public RestException(int statusCode, String message) {
        super(statusCode, message);
        this.message = Messages.INTERNAL_ERROR;
    }

    public RestException(String message) {
        super(500, message);
        this.message = Messages.INTERNAL_ERROR;
    }

    public RestException(Throwable cause) {
        super(500, unwrap(cause));
        message = Messages.INTERNAL_ERROR;
    }

    public String getErrorCode() {
        return message.name();
    }

    public static RuntimeException wrap(Throwable t) {
        t = unwrap(t);
        return t instanceof RuntimeException ? (RuntimeException) t : new RestException(t);
    }

    public static Throwable unwrap(Throwable t) {
        while (true) {
            if (t instanceof UndeclaredThrowableException) {
                t = ((UndeclaredThrowableException) t).getUndeclaredThrowable();
            } else if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            } else if (t instanceof CompletionException || t instanceof ExecutionException) {
                Throwable cause = t.getCause();
                if (cause == t) {
                    break;
                }
                t = cause;
            } else {
                break;
            }
        }
        return t;
    }
}
