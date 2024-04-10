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
package org.apache.dubbo.xds.security.authz;

import org.apache.dubbo.rpc.Invocation;

import java.util.LinkedList;

public class AuthorizationRequestContext {

    private final Invocation invocation;

    /**
     * 凭据
     */
    private final RequestCredential requestCredential;

    private boolean failed = false;

    private Exception validationException;

    private LinkedList<String> currentMapPath = new LinkedList<>();

    public AuthorizationRequestContext(Invocation invocation, RequestCredential requestCredential) {
        this.invocation = invocation;
        this.requestCredential = requestCredential;
    }

    public String getCurrentPath() {
        return String.join(".", currentMapPath);
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public Exception getValidationException() {
        return validationException;
    }

    public void setValidationException(Exception validationException) {
        this.validationException = validationException;
    }

    public RequestCredential getRequestCredential() {
        return requestCredential;
    }
}
