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
import org.apache.dubbo.remoting.exchange.Response;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;

/**
 * See https://github.com/grpc/grpc/blob/master/doc/statuscodes.md
 */

public class GrpcStatus {
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

    public static byte toDubboStatus(Code code) {
        byte status;
        switch (code) {
            case OK:
                status = Response.OK;
                break;
            case UNKNOWN:
                status = Response.SERVICE_ERROR;
                break;
            case DEADLINE_EXCEEDED:
                status = Response.SERVER_TIMEOUT;
                break;
            case RESOURCE_EXHAUSTED:
                status = Response.SERVER_THREADPOOL_EXHAUSTED_ERROR;
                break;
            case UNIMPLEMENTED:
                status = Response.SERVICE_NOT_FOUND;
                break;
            case INVALID_ARGUMENT:
                status = Response.BAD_REQUEST;
                break;
            case INTERNAL:
                status = Response.SERVER_ERROR;
                break;
            case UNAVAILABLE:
            case DATA_LOSS:
                status = Response.CHANNEL_INACTIVE;
                break;
            default:
                status = Response.CLIENT_ERROR;
                break;
        }
        return status;
    }

    public static String limitSizeTo4KB(String desc) {
        if (desc.length() < 4096) {
            return desc;
        } else {
            return desc.substring(0, 4086);
        }
    }

    public static String fromMessage(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return QueryStringDecoder.decodeComponent(raw);
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

    public String toMessage() {
        final String msg;
        if (cause == null) {
            msg = description;
        } else {
            String placeHolder = description == null ? "" : description;
            msg = StringUtils.toString(placeHolder, cause);
        }
        if (msg == null) {
            return "";
        }
        String output = limitSizeTo4KB(msg);
        QueryStringEncoder encoder = new QueryStringEncoder("");
        encoder.addParam("", output);
        // ?=
        return encoder.toString().substring(2);
    }

    enum Code {
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
        DATA_LOSS(15);

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
