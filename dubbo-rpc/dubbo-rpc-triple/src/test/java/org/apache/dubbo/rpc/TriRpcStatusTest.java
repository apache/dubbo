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

import java.io.Serializable;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
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
            // pass
        }
    }

    @Test
    void testFromCode() {
        Assertions.assertEquals(Code.UNKNOWN, TriRpcStatus.fromCode(Code.UNKNOWN).code);
    }

    @Test
    void getStatus() {
        StatusRpcException rpcException = new StatusRpcException(TriRpcStatus.INTERNAL);
        Assertions.assertEquals(TriRpcStatus.INTERNAL.code, TriRpcStatus.getStatus(rpcException).code);
    }

    @Test
    void testGetStatus() {
        StatusRpcException rpcException = new StatusRpcException(TriRpcStatus.INTERNAL);
        Assertions.assertEquals(TriRpcStatus.INTERNAL.code, TriRpcStatus.getStatus(rpcException, null).code);

        Assertions.assertEquals(
                TriRpcStatus.DEADLINE_EXCEEDED.code,
                TriRpcStatus.getStatus(new RpcException(RpcException.TIMEOUT_EXCEPTION), null).code);

        Assertions.assertEquals(
                TriRpcStatus.DEADLINE_EXCEEDED.code,
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
        Assertions.assertEquals(message, TriRpcStatus.decodeMessage(TriRpcStatus.encodeMessage(message)));

        Assertions.assertTrue(TriRpcStatus.decodeMessage("").isEmpty());
        Assertions.assertTrue(TriRpcStatus.decodeMessage(null).isEmpty());
    }

    @Test
    void decodeMessageKeepsPlusSign() {
        // grpc-java and grpc-go do not percent-encode '+' in grpc-message
        Assertions.assertEquals("a+b", TriRpcStatus.decodeMessage("a+b"));
        Assertions.assertEquals("C++ parser, tz +08:00", TriRpcStatus.decodeMessage("C++ parser, tz +08:00"));
        Assertions.assertEquals("中文 a+b", TriRpcStatus.decodeMessage("%E4%B8%AD%E6%96%87 a+b"));
    }

    @Test
    void decodeMessageKeepsInvalidPercentEncoding() {
        // gRPC: when decoding invalid values, implementations MUST NOT error or throw away the message
        Assertions.assertEquals("quota 100% used", TriRpcStatus.decodeMessage("quota 100% used"));
        Assertions.assertEquals("100%", TriRpcStatus.decodeMessage("100%"));
        Assertions.assertEquals("%4", TriRpcStatus.decodeMessage("%4"));
        Assertions.assertEquals("%zz %4g %g4", TriRpcStatus.decodeMessage("%zz %4g %g4"));
        Assertions.assertEquals("90% 中文", TriRpcStatus.decodeMessage("90% %E4%B8%AD%E6%96%87"));
        // status details carry the message without percent-encoding
        Assertions.assertEquals("中文 50% off, café", TriRpcStatus.decodeMessage("中文 50% off, café"));
    }

    @Test
    void decodeMessageDecodesPercentEncodedUtf8() {
        Assertions.assertEquals("中文 message", TriRpcStatus.decodeMessage("%E4%B8%AD%E6%96%87 message"));
        Assertions.assertEquals("中", TriRpcStatus.decodeMessage("%e4%b8%ad"));
        Assertions.assertEquals("😯 100%", TriRpcStatus.decodeMessage("%F0%9F%98%AF 100%25"));
        for (String message : new String[] {"a+b", "a b", "50% off", "%E4%B8%AD", "中文", "line1\nline2"}) {
            Assertions.assertEquals(message, TriRpcStatus.decodeMessage(TriRpcStatus.encodeMessage(message)));
        }
    }

    @Test
    void httpStatusToGrpcCode() {
        Assertions.assertEquals(Code.UNIMPLEMENTED, TriRpcStatus.httpStatusToGrpcCode(404));
        Assertions.assertEquals(
                Code.UNAVAILABLE, TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.BAD_GATEWAY.code()));
        Assertions.assertEquals(
                Code.UNAVAILABLE, TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.TOO_MANY_REQUESTS.code()));
        Assertions.assertEquals(
                Code.UNAVAILABLE, TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        Assertions.assertEquals(
                Code.UNAVAILABLE, TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.GATEWAY_TIMEOUT.code()));
        Assertions.assertEquals(Code.INTERNAL, TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.CONTINUE.code()));
        Assertions.assertEquals(
                Code.INTERNAL,
                TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.code()));
        Assertions.assertEquals(Code.UNKNOWN, TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.ACCEPTED.code()));
        Assertions.assertEquals(
                Code.PERMISSION_DENIED, TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.FORBIDDEN.code()));
        Assertions.assertEquals(
                Code.UNIMPLEMENTED, TriRpcStatus.httpStatusToGrpcCode(HttpResponseStatus.NOT_FOUND.code()));
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
                .withCause(new IllegalStateException("test"))
                .asException();
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
        Assertions.assertTrue(message.chars().allMatch(c -> c > ' ' && c <= '~'));
        Assertions.assertTrue(message.contains("IllegalStateException:%20test"));
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
    void encodeMessageKeepsPrintableAscii() {
        // grpc-js decodes grpc-message with decodeURI, which leaves escapes such as %3A and %2F undecoded
        Assertions.assertEquals("order%2042:%20not%20found", TriRpcStatus.encodeMessage("order 42: not found"));
        for (char c = '!'; c <= '~'; c++) {
            if (c != '%' && c != '+') {
                Assertions.assertEquals(String.valueOf(c), TriRpcStatus.encodeMessage(String.valueOf(c)));
            }
        }
    }

    @Test
    void encodeMessageEscapesWhitespacePercentPlusAndNonAscii() {
        Assertions.assertEquals("%20a%09b%0A%20", TriRpcStatus.encodeMessage(" a\tb\n "));
        Assertions.assertEquals("done%20", TriRpcStatus.encodeMessage("done "));
        Assertions.assertEquals("100%25", TriRpcStatus.encodeMessage("100%"));
        // '+' stays escaped: Dubbo 3.0 to 3.3.7 consumers decode it as a space
        Assertions.assertEquals("50%25%2B1", TriRpcStatus.encodeMessage("50%+1"));
        Assertions.assertEquals("%E4%B8%AD%F0%9F%98%AF%7F", TriRpcStatus.encodeMessage("中😯\u007f"));
        for (String message :
                new String[] {"order 42: not found", "C++ a+b 50% done", " 中文 café 😯 ", "%E4%B8%AD", "a\r\nb"}) {
            String encoded = TriRpcStatus.encodeMessage(message);
            Assertions.assertEquals(message, TriRpcStatus.decodeMessage(encoded));
            // what Dubbo 3.0 to 3.3.7 consumers use
            Assertions.assertEquals(message, QueryStringDecoder.decodeComponent(encoded));
        }
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
        final TriRpcStatus status = TriRpcStatus.INTERNAL.withDescription(content);
        Assertions.assertEquals(content, status.toMessage());
    }

    @Test
    void triCodeToDubboCode() {
        Assertions.assertEquals(TIMEOUT_EXCEPTION, TriRpcStatus.triCodeToDubboCode(Code.DEADLINE_EXCEEDED));
        Assertions.assertEquals(FORBIDDEN_EXCEPTION, TriRpcStatus.triCodeToDubboCode(Code.PERMISSION_DENIED));
        Assertions.assertEquals(METHOD_NOT_FOUND, TriRpcStatus.triCodeToDubboCode(Code.UNIMPLEMENTED));
        Assertions.assertEquals(UNKNOWN_EXCEPTION, TriRpcStatus.triCodeToDubboCode(Code.UNKNOWN));
    }

    @Test
    void testSerializable() {
        TriRpcStatus status = TriRpcStatus.INTERNAL.withDescription("test");
        Assertions.assertInstanceOf(Serializable.class, status.asException());
        Assertions.assertInstanceOf(Serializable.class, status.asException().getStatus());
    }
}
