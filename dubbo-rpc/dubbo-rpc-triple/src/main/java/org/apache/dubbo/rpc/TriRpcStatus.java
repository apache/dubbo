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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import io.netty.handler.codec.http.HttpResponseStatus;

import static org.apache.dubbo.rpc.RpcException.AUTHORIZATION_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.FORBIDDEN_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.LIMIT_EXCEEDED_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.METHOD_NOT_FOUND;
import static org.apache.dubbo.rpc.RpcException.NETWORK_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.SERIALIZATION_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.TIMEOUT_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.TIMEOUT_TERMINATE;
import static org.apache.dubbo.rpc.RpcException.UNKNOWN_EXCEPTION;

/**
 * See <a href="https://github.com/grpc/grpc/blob/master/doc/statuscodes.md">status codes</a>
 */
public class TriRpcStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    public static final TriRpcStatus OK = fromCode(Code.OK);
    public static final TriRpcStatus UNKNOWN = fromCode(Code.UNKNOWN);
    public static final TriRpcStatus INTERNAL = fromCode(Code.INTERNAL);
    public static final TriRpcStatus NOT_FOUND = fromCode(Code.NOT_FOUND);
    public static final TriRpcStatus CANCELLED = fromCode(Code.CANCELLED);
    public static final TriRpcStatus UNAVAILABLE = fromCode(Code.UNAVAILABLE);
    public static final TriRpcStatus UNIMPLEMENTED = fromCode(Code.UNIMPLEMENTED);
    public static final TriRpcStatus DEADLINE_EXCEEDED = fromCode(Code.DEADLINE_EXCEEDED);

    public final Code code;
    public final Throwable cause;
    public final String description;

    public TriRpcStatus(Code code, Throwable cause, String description) {
        this.code = code;
        this.cause = cause;
        this.description = description;
    }

    public static TriRpcStatus fromCode(int code) {
        return fromCode(Code.fromCode(code));
    }

    public static TriRpcStatus fromCode(Code code) {
        return new TriRpcStatus(code, null, null);
    }

    /**
     * todo The remaining exceptions are converted to status
     */
    public static TriRpcStatus getStatus(Throwable throwable) {
        return getStatus(throwable, null);
    }

    public static TriRpcStatus getStatus(Throwable throwable, String description) {
        if (throwable instanceof HttpStatusException) {
            int statusCode = ((HttpStatusException) throwable).getStatusCode();
            return new TriRpcStatus(httpStatusToGrpcCode(statusCode), throwable, description);
        }
        if (throwable instanceof StatusRpcException) {
            return ((StatusRpcException) throwable).getStatus();
        }
        if (throwable instanceof RpcException) {
            RpcException rpcException = (RpcException) throwable;
            Code code = dubboCodeToTriCode(rpcException.getCode());
            return new TriRpcStatus(code, throwable, description);
        }
        if (throwable instanceof TimeoutException) {
            return new TriRpcStatus(Code.DEADLINE_EXCEEDED, throwable, description);
        }
        return new TriRpcStatus(Code.UNKNOWN, throwable, description);
    }

    public static int triCodeToDubboCode(Code triCode) {
        int code;
        switch (triCode) {
            case DEADLINE_EXCEEDED:
                code = TIMEOUT_EXCEPTION;
                break;
            case PERMISSION_DENIED:
                code = FORBIDDEN_EXCEPTION;
                break;
            case UNAVAILABLE:
                code = NETWORK_EXCEPTION;
                break;
            case UNIMPLEMENTED:
                code = METHOD_NOT_FOUND;
                break;
            default:
                code = UNKNOWN_EXCEPTION;
        }
        return code;
    }

    public static Code dubboCodeToTriCode(int rpcExceptionCode) {
        Code code;
        switch (rpcExceptionCode) {
            case TIMEOUT_EXCEPTION:
            case TIMEOUT_TERMINATE:
                code = Code.DEADLINE_EXCEEDED;
                break;
            case FORBIDDEN_EXCEPTION:
                code = Code.PERMISSION_DENIED;
                break;
            case AUTHORIZATION_EXCEPTION:
                code = Code.UNAUTHENTICATED;
                break;
            case LIMIT_EXCEEDED_EXCEPTION:
            case NETWORK_EXCEPTION:
                code = Code.UNAVAILABLE;
                break;
            case METHOD_NOT_FOUND:
                code = Code.UNIMPLEMENTED;
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

    public static String limitSizeTo1KB(String desc) {
        if (desc.length() < 1024) {
            return desc;
        } else {
            return desc.substring(0, 1024);
        }
    }

    /**
     * Percent-decodes a value written by {@link #encodeMessage(String)} or received as grpc-message. As in gRPC,
     * '+' stays '+', and an invalid percent sequence is kept as is instead of failing the decoding.
     */
    public static String decodeMessage(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return "";
        }
        int start = raw.indexOf('%');
        if (start < 0) {
            return raw;
        }
        int length = raw.length();
        StringBuilder builder = new StringBuilder(length).append(raw, 0, start);
        byte[] bytes = new byte[length - start];
        int size = 0;
        for (int i = start; i < length; i++) {
            char c = raw.charAt(i);
            if (c == '%' && i + 2 < length) {
                int high = StringUtils.decodeHexNibble(raw.charAt(i + 1));
                int low = StringUtils.decodeHexNibble(raw.charAt(i + 2));
                if (high != -1 && low != -1) {
                    bytes[size++] = (byte) ((high << 4) + low);
                    i += 2;
                    continue;
                }
            }
            if (c < 0x80) {
                bytes[size++] = (byte) c;
                continue;
            }
            if (size > 0) {
                builder.append(new String(bytes, 0, size, StandardCharsets.UTF_8));
                size = 0;
            }
            builder.append(c);
        }
        if (size > 0) {
            builder.append(new String(bytes, 0, size, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    /**
     * Percent-encodes a value for grpc-message or tri-header-convert: the UTF-8 bytes outside printable ASCII, '%',
     * the space, so that the value never starts or ends with whitespace, and '+', which Dubbo 3.0 to 3.3.7 decode as
     * a space. Other printable ASCII stays as is, because grpc-js decodes grpc-message with decodeURI, which leaves
     * escapes such as %3A and %2F in place.
     */
    public static String encodeMessage(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return "";
        }
        int length = raw.length();
        int i = 0;
        while (i < length && !isEscapedInMessage(raw.charAt(i))) {
            i++;
        }
        if (i == length) {
            return raw;
        }
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder(bytes.length + 16);
        for (byte b : bytes) {
            int c = b & 0xFF;
            if (isEscapedInMessage(c)) {
                builder.append('%').append(HEX_DIGITS[c >> 4]).append(HEX_DIGITS[c & 0xF]);
            } else {
                builder.append((char) c);
            }
        }
        return builder.toString();
    }

    private static boolean isEscapedInMessage(int c) {
        return c <= ' ' || c > '~' || c == '%' || c == '+';
    }

    public static Code httpStatusToGrpcCode(int httpStatusCode) {
        if (httpStatusCode >= 100 && httpStatusCode < 200) {
            return Code.INTERNAL;
        }
        if (httpStatusCode == HttpResponseStatus.BAD_REQUEST.code()
                || httpStatusCode == HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code()) {
            return Code.INTERNAL;
        } else if (httpStatusCode == HttpResponseStatus.UNAUTHORIZED.code()) {
            return Code.UNAUTHENTICATED;
        } else if (httpStatusCode == HttpResponseStatus.FORBIDDEN.code()) {
            return Code.PERMISSION_DENIED;
        } else if (httpStatusCode == HttpResponseStatus.NOT_FOUND.code()) {
            return Code.UNIMPLEMENTED;
        } else if (httpStatusCode == HttpResponseStatus.BAD_GATEWAY.code()
                || httpStatusCode == HttpResponseStatus.TOO_MANY_REQUESTS.code()
                || httpStatusCode == HttpResponseStatus.SERVICE_UNAVAILABLE.code()
                || httpStatusCode == HttpResponseStatus.GATEWAY_TIMEOUT.code()) {
            return Code.UNAVAILABLE;
        } else {
            return Code.UNKNOWN;
        }
    }

    public static int grpcCodeToHttpStatus(Code code) {
        switch (code) {
            case OK:
                return HttpResponseStatus.OK.code();
            case CANCELLED:
                return 499;
            case UNKNOWN:
            case DATA_LOSS:
            case INTERNAL:
                return HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
            case INVALID_ARGUMENT:
            case FAILED_PRECONDITION:
            case OUT_OF_RANGE:
                return HttpResponseStatus.BAD_REQUEST.code();
            case DEADLINE_EXCEEDED:
                return HttpResponseStatus.GATEWAY_TIMEOUT.code();
            case NOT_FOUND:
                return HttpResponseStatus.NOT_FOUND.code();
            case ALREADY_EXISTS:
            case ABORTED:
                return HttpResponseStatus.CONFLICT.code();
            case PERMISSION_DENIED:
                return HttpResponseStatus.FORBIDDEN.code();
            case RESOURCE_EXHAUSTED:
                return HttpResponseStatus.TOO_MANY_REQUESTS.code();
            case UNIMPLEMENTED:
                return HttpResponseStatus.NOT_IMPLEMENTED.code();
            case UNAVAILABLE:
                return HttpResponseStatus.SERVICE_UNAVAILABLE.code();
            case UNAUTHENTICATED:
                return HttpResponseStatus.UNAUTHORIZED.code();
            default:
                return -1;
        }
    }

    public boolean isOk() {
        return Code.isOk(code.code);
    }

    public TriRpcStatus withCause(Throwable cause) {
        return new TriRpcStatus(this.code, cause, this.description);
    }

    public TriRpcStatus withDescription(String description) {
        return new TriRpcStatus(code, cause, description);
    }

    public TriRpcStatus appendDescription(String description) {
        if (this.description == null) {
            return withDescription(description);
        } else {
            String newDescription = this.description + "\n" + description;
            return withDescription(newDescription);
        }
    }

    public StatusRpcException asException() {
        return new StatusRpcException(this);
    }

    public String toEncodedMessage() {
        String output = limitSizeTo1KB(toMessage());
        return encodeMessage(output);
    }

    public String toMessageWithoutCause() {
        if (description != null) {
            return String.format("%s : %s", code, description);
        } else {
            return code.toString();
        }
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
         * The request does not have valid authentication credentials for the operation.
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
