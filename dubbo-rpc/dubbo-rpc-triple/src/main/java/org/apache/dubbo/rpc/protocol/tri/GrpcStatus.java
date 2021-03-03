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

/**
 * See https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
 */

public class GrpcStatus {
    public static final GrpcStatus OK = new GrpcStatus(Code.OK, null, "OK");
    public final Code code;
    public final Throwable cause;
    public final String description;

    public GrpcStatus(Code code, Throwable cause, String description) {
        this.code = code;
        this.cause = cause;
        this.description = description;
    }

    public static GrpcStatus fromCode(int code) {
        return fromCode(Code.fromCode(code));
    }

    public static GrpcStatus fromCode(Code code) {
        return new GrpcStatus(code, null, null);
    }

    public GrpcStatus withCause(Throwable cause) {
        return new GrpcStatus(this.code, cause, this.description);
    }

    public GrpcStatus withDescription(String description) {
        return new GrpcStatus(this.code, this.cause, description);
    }

    public TripleRpcException asException() {
        return new TripleRpcException(this);
    }

    enum Code {
        OK(0),
        UNKNOWN(2),
        DEADLINE_EXCEEDED(4),
        NOT_FOUND(5),
        RESOURCE_EXHAUSTED(8),
        UNIMPLEMENTED(12),
        INTERNAL(13);

        final int code;

        Code(int code) {
            this.code = code;
        }

        public static boolean isOk(Integer status) {
            return status == OK.code;
        }

        public static Code fromCode(int code) {
            for (Code value : Code.values()) {
                if (value.code == code) {
                    return value;
                }
            }
            throw new IllegalStateException("Can not find status for code: " + code);
        }
    }

}
