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

import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.TriRpcStatus.Code;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.rpc.RpcException.FORBIDDEN_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.METHOD_NOT_FOUND;
import static org.apache.dubbo.rpc.RpcException.TIMEOUT_EXCEPTION;
import static org.apache.dubbo.rpc.RpcException.UNKNOWN_EXCEPTION;
import static org.junit.jupiter.api.Assertions.fail;

class TriRpcStatusTest {

    @Test
    void fromCode() {
        Assertions.assertEquals(Code.UNKNOWN, TriRpcStatus.fromCode(2).code);
        try {
            TriRpcStatus.fromCode(1000);
            fail();
        } catch (Throwable t) {
            //pass
        }
    }

    @Test
    void testFromCode() {
        Assertions.assertEquals(Code.UNKNOWN, TriRpcStatus.fromCode(Code.UNKNOWN).code);
    }

    @Test
    void getStatus() {
        StatusRpcException rpcException = new StatusRpcException(TriRpcStatus.INTERNAL);
        Assertions.assertEquals(TriRpcStatus.INTERNAL.code,
            TriRpcStatus.getStatus(rpcException).code);
    }

    @Test
    void testGetStatus() {
        StatusRpcException rpcException = new StatusRpcException(TriRpcStatus.INTERNAL);
        Assertions.assertEquals(TriRpcStatus.INTERNAL.code,
            TriRpcStatus.getStatus(rpcException, null).code);

        Assertions.assertEquals(TriRpcStatus.DEADLINE_EXCEEDED.code,
            TriRpcStatus.getStatus(new RpcException(RpcException.TIMEOUT_EXCEPTION), null).code);

        Assertions.assertEquals(TriRpcStatus.DEADLINE_EXCEEDED.code,
            TriRpcStatus.getStatus(new TimeoutException(true, null, null), null).code);
    }

    @Test
    void rpcExceptionCodeToGrpcCode() {
        Assertions.assertEquals(Code.DEADLINE_EXCEEDED, TriRpcStatus.dubboCodeToTriCode(2));
    }

    @Test
    void limitSizeTo1KB() {
        String a = "a";
        for (int i = 0; i < 11; i++) {
            a += a;
        }
        Assertions.assertEquals(1024, TriRpcStatus.limitSizeTo1KB(a).length());
        Assertions.assertEquals(1, TriRpcStatus.limitSizeTo1KB("a").length());
    }

    @Test
    void decodeMessage() {
        String message = "😯";
        Assertions.assertEquals(message,
            TriRpcStatus.decodeMessage(TriRpcStatus.encodeMessage(message)));

        Assertions.assertTrue(TriRpcStatus.decodeMessage("").isEmpty());
        Assertions.assertTrue(TriRpcStatus.decodeMessage(null).isEmpty());
    }

    @Test
    void httpStatusToGrpcCode() {
        Assertions.assertEquals(Code.UNIMPLEMENTED, TriRpcStatus.httpStatusToGrpcCode(404));
        Assertions.assertEquals(Code.UNAVAILABLE,
            TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.BAD_GATEWAY.code()));
        Assertions.assertEquals(Code.UNAVAILABLE,
            TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.TOO_MANY_REQUESTS.code()));
        Assertions.assertEquals(Code.UNAVAILABLE,
            TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        Assertions.assertEquals(Code.UNAVAILABLE,
            TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.GATEWAY_TIMEOUT.code()));
        Assertions.assertEquals(Code.INTERNAL,
            TriRpcStatus.httpStatusToGrpcCode(
                HttpResponseStatus.CONTINUE.code()));
        Assertions.assertEquals(Code.INTERNAL,
            TriRpcStatus.httpStatusToGrpcCode(
                HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code()));
        Assertions.assertEquals(Code.UNKNOWN,
            TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.ACCEPTED.code()));
        Assertions.assertEquals(Code.PERMISSION_DENIED,
            TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.FORBIDDEN.code()));
        Assertions.assertEquals(Code.UNIMPLEMENTED,
            TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.NOT_FOUND.code()));
    }

    @Test
    void isOk() {
        Assertions.assertTrue(TriRpcStatus.OK.isOk());
        Assertions.assertFalse(TriRpcStatus.NOT_FOUND.isOk());
    }

    @Test
    void withCause() {
        TriRpcStatus origin = TriRpcStatus.NOT_FOUND;
        TriRpcStatus withCause = origin.withCause(new IllegalStateException("test"));
        Assertions.assertNull(origin.cause);
        Assertions.assertTrue(withCause.cause.getMessage().contains("test"));
    }

    @Test
    void withDescription() {
        TriRpcStatus origin = TriRpcStatus.NOT_FOUND;
        TriRpcStatus withDesc = origin.withDescription("desc");
        Assertions.assertNull(origin.description);
        Assertions.assertTrue(withDesc.description.contains("desc"));
    }

    @Test
    void appendDescription() {
        TriRpcStatus origin = TriRpcStatus.NOT_FOUND;
        TriRpcStatus withDesc = origin.appendDescription("desc0");
        TriRpcStatus withDesc2 = withDesc.appendDescription("desc1");

        Assertions.assertNull(origin.description);
        Assertions.assertTrue(withDesc2.description.contains("desc1"));
        Assertions.assertTrue(withDesc2.description.contains("desc0"));
    }

    @Test
    void asException() {
        StatusRpcException exception = TriRpcStatus.NOT_FOUND
            .withDescription("desc")
            .withCause(new IllegalStateException("test")).asException();
        Assertions.assertEquals(Code.NOT_FOUND, exception.getStatus().code);
    }

    @Test
    void toEncodedMessage() {
        String message = TriRpcStatus.NOT_FOUND
            .withDescription("desc")
            .withCause(new IllegalStateException("test"))
            .toEncodedMessage();
        Assertions.assertTrue(message.contains("desc"));
        Assertions.assertTrue(message.contains("test"));
    }

    @Test
    void toMessageWithoutCause() {
        String message = TriRpcStatus.NOT_FOUND
            .withDescription("desc")
            .withCause(new IllegalStateException("test"))
            .toMessageWithoutCause();
        Assertions.assertTrue(message.contains("desc"));
        Assertions.assertFalse(message.contains("test"));
    }

    @Test
    void toMessage() {
        String message = TriRpcStatus.NOT_FOUND
            .withDescription("desc")
            .withCause(new IllegalStateException("test"))
            .toMessage();
        Assertions.assertTrue(message.contains("desc"));
        Assertions.assertTrue(message.contains("test"));
    }

    @Test
    void encodeMessage() {
        Assertions.assertTrue(TriRpcStatus.encodeMessage(null).isEmpty());
        Assertions.assertTrue(TriRpcStatus.encodeMessage("").isEmpty());
    }

    @Test
    void fromMessage() {
        String origin = "haha test 😊";
        final String encoded = TriRpcStatus.encodeMessage(origin);
        Assertions.assertNotEquals(origin, encoded);
        final String decoded = TriRpcStatus.decodeMessage(encoded);
        Assertions.assertEquals(origin, decoded);
    }

    @Test
    void toMessage2() {
        String content = "\t\ntest with whitespace\r\nand Unicode BMP ☺ and non-BMP 😈\t\n";
        final TriRpcStatus status = TriRpcStatus.INTERNAL
            .withDescription(content);
        Assertions.assertEquals(content, status.toMessage());
    }

    @Test
    void triCodeToDubboCode() {
        Assertions.assertEquals(TIMEOUT_EXCEPTION,
            TriRpcStatus.triCodeToDubboCode(Code.DEADLINE_EXCEEDED));
        Assertions.assertEquals(FORBIDDEN_EXCEPTION,
            TriRpcStatus.triCodeToDubboCode(Code.PERMISSION_DENIED));
        Assertions.assertEquals(METHOD_NOT_FOUND,
            TriRpcStatus.triCodeToDubboCode(Code.UNIMPLEMENTED));
        Assertions.assertEquals(UNKNOWN_EXCEPTION, TriRpcStatus.triCodeToDubboCode(Code.UNKNOWN));
    }
}
