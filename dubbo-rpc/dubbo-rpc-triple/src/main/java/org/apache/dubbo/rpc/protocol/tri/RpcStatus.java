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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.RpcException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;

import static org.apache.dubbo.rpc.RpcException.FORBIDDEN_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.LIMIT_EXCEEDED_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.METHOD_NOT_FOUND;
import static org.apache.dubbo.rpc.RpcException.NETWORK_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.SERIALIZATION_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.TIMEOUT_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.TIMEOUT_TERMINATE;

/**
 * See https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
 */

public class RpcStatus {
    public static final RpcStatus OK = fromCode(Code.OK);
    public static final RpcStatus UNKNOWN = fromCode(Code.UNKNOWN);
    public static final RpcStatus INTERNAL = fromCode(Code.INTERNAL);
    public static final RpcStatus CANCELLED = fromCode(Code.CANCELLED);
    public static final RpcStatus UNAVAILABLE = fromCode(Code.UNAVAILABLE);
    public static final RpcStatus UNIMPLEMENTED = fromCode(Code.UNIMPLEMENTED);
    public static final RpcStatus DEADLINE_EXCEEDED = fromCode(Code.DEADLINE_EXCEEDED);

    public final Code code;
    public final Throwable cause;
    public final String description;

    public RpcStatus(Code code, Throwable cause, String description) {
        this.code = code;
        this.cause = cause;
        this.description = description;
    }

    public static RpcStatus fromCode(int code) {
        return fromCode(Code.fromCode(code));
    }

    public static RpcStatus fromCode(Code code) {
        return new RpcStatus(code, null, null);
    }

    /**
     * todo The remaining exceptions are converted to status
     */
    public static RpcStatus getStatus(Throwable throwable) {
        return getStatus(throwable, null);
    }

    public static RpcStatus getStatus(Throwable throwable, String description) {
        if (throwable instanceof RpcException) {
            RpcException rpcException = (RpcException) throwable;
            Code code = rpcExceptionCodeToGrpcCode(rpcException.getCode());
            return new RpcStatus(code, throwable, description);
        }
        if (throwable instanceof TimeoutException) {
            return new RpcStatus(RpcStatus.Code.DEADLINE_EXCEEDED, throwable, description);
        }
        return new RpcStatus(Code.UNKNOWN, throwable, description);
    }

    public static Code rpcExceptionCodeToGrpcCode(int rpcExceptionCode) {
        Code code;
        switch (rpcExceptionCode) {
            case TIMEOUT_EXCEPTION:
            case TIMEOUT_TERMINATE:
                code = Code.DEADLINE_EXCEEDED;
                break;
            case FORBIDDEN_EXCEPTION:
                code = Code.PERMISSION_DENIED;
                break;
            case LIMIT_EXCEEDED_EXCEPTION:
            case NETWORK_EXCEPTION:
                code = Code.UNAVAILABLE;
                break;
            case METHOD_NOT_FOUND:
                code = Code.NOT_FOUND;
                break;
            case SERIALIZATION_EXCEPTION:
                code = Code.INTERNAL;
                break;
            default:
                code = Code.UNKNOWN;
                break;
        }
        return code;
    }

    public static String limitSizeTo4KB(String desc) {
        if (desc.length() < 4096) {
            return desc;
        } else {
            return desc.substring(0, 4086);
        }
    }

    public static String decodeMessage(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return "";
        }
        return QueryStringDecoder.decodeComponent(raw);
    }

    public static String encodeMessage(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return "";
        }
        return encodeComponent(raw);
    }

    private static String encodeComponent(String raw) {
        QueryStringEncoder encoder = new QueryStringEncoder("");
        encoder.addParam("", raw);
        // ?=
        return encoder.toString().substring(2);
    }

    public static RpcStatus.Code httpStatusToGrpcCode(int httpStatusCode) {
        if (httpStatusCode >= 100 && httpStatusCode < 200) {
            return RpcStatus.Code.INTERNAL;
        }
        if (httpStatusCode == HttpResponseStatus.BAD_REQUEST.code() ||
            httpStatusCode == HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code()
        ) {
            return RpcStatus.Code.INTERNAL;
        } else if (httpStatusCode == HttpResponseStatus.UNAUTHORIZED.code()) {
            return RpcStatus.Code.UNAUTHENTICATED;
        } else if (httpStatusCode == HttpResponseStatus.FORBIDDEN.code()) {
            return RpcStatus.Code.PERMISSION_DENIED;
        } else if (httpStatusCode == HttpResponseStatus.NOT_FOUND.code()) {
            return RpcStatus.Code.UNIMPLEMENTED;
        } else if (httpStatusCode == HttpResponseStatus.BAD_GATEWAY.code()
            || httpStatusCode == HttpResponseStatus.TOO_MANY_REQUESTS.code()
            || httpStatusCode == HttpResponseStatus.SERVICE_UNAVAILABLE.code()
            || httpStatusCode == HttpResponseStatus.GATEWAY_TIMEOUT.code()) {
            return Code.UNAVAILABLE;
        } else {
            return RpcStatus.Code.UNKNOWN;
        }
    }

    public boolean isOk() {
        return Code.isOk(code.code);
    }

    public RpcStatus withCause(Throwable cause) {
        return new RpcStatus(this.code, cause, this.description);
    }

    public RpcStatus withDescription(String description) {
        return new RpcStatus(code, cause, description);
    }

    public RpcStatus appendDescription(String description) {
        if (this.description == null) {
            return withDescription(description);
        } else {
            String newDescription = this.description + "\n" + description;
            return withDescription(newDescription);
        }
    }

    public RpcException asException() {
        return new RpcException(this.code.code, this.description, this.cause);
    }

    public String toEncodedMessage() {
        String output = limitSizeTo4KB(toMessage());
        return encodeComponent(output);
    }

    public String toMessage() {
        String msg = "";
        if (cause == null) {
            msg += description;
        } else {
            String placeHolder = description == null ? "" : description;
            msg += StringUtils.toString(placeHolder, cause);
        }
        return msg;
    }


    public enum Code {
        OK(0),
        CANCELLED(1),
        UNKNOWN(2),
        INVALID_ARGUMENT(3),
        DEADLINE_EXCEEDED(4),
        NOT_FOUND(5),
        ALREADY_EXISTS(6),
        PERMISSION_DENIED(7),
        RESOURCE_EXHAUSTED(8),
        FAILED_PRECONDITION(9),
        ABORTED(10),
        OUT_OF_RANGE(11),
        UNIMPLEMENTED(12),
        INTERNAL(13),
        UNAVAILABLE(14),
        DATA_LOSS(15),
        /**
         * The request does not have valid authentication credentials for the
         * operation.
         */
        UNAUTHENTICATED(16);

        public final int code;

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
