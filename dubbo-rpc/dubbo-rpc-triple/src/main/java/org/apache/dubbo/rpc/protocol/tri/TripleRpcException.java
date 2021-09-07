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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.rpc.RpcException;

public class TripleRpcException extends RpcException {
    private final GrpcStatus status;
    private final Metadata trailers;
    private final boolean fillInStackTrace;
    private int code;

    public TripleRpcException(int code, String msg) {
        this(code, msg, null);
    }

    public TripleRpcException(int code, String msg, Metadata trailers) {
        super(msg);
        this.code = code;
        this.status = null;
        this.trailers = trailers;
        this.fillInStackTrace = false;
    }

    public TripleRpcException(GrpcStatus status) {
        this(status, null);
    }

    public TripleRpcException(GrpcStatus status, Metadata trailers) {
        this(status, trailers, true);
    }

    public TripleRpcException(GrpcStatus status, Metadata trailers, boolean fillInStackTrace) {
        super(status.description, status.cause);
        this.status = status;
        this.trailers = trailers;
        this.fillInStackTrace = fillInStackTrace;
        this.code = status.code.code;
        fillInStackTrace();
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return fillInStackTrace ? super.fillInStackTrace() : this;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public void setCode(int code) {
        this.code = code;
    }

    public GrpcStatus getStatus() {
        return status;
    }

    public Metadata getTrailers() {
        return trailers;
    }
}
