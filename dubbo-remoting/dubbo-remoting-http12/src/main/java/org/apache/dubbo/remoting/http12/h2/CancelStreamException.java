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
package org.apache.dubbo.remoting.http12.h2;

import org.apache.dubbo.remoting.http12.ErrorCodeHolder;

public class CancelStreamException extends RuntimeException implements ErrorCodeHolder {

    private static final long serialVersionUID = 1L;

    private final boolean cancelByRemote;
    private final long errorCode;

    private CancelStreamException(boolean cancelByRemote, long errorCode) {
        this.cancelByRemote = cancelByRemote;
        this.errorCode = errorCode;
    }

    public boolean isCancelByRemote() {
        return cancelByRemote;
    }

    public static CancelStreamException fromRemote(long errorCode) {
        return new CancelStreamException(true, errorCode);
    }

    public static CancelStreamException fromLocal(long errorCode) {
        return new CancelStreamException(false, errorCode);
    }

    @Override
    public long getErrorCode() {
        return errorCode;
    }
}
