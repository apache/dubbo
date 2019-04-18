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
package org.apache.dubbo.remoting.etcd.option;

import io.etcd.jetcd.ByteSequence;
import io.grpc.Status;
import io.netty.handler.codec.http2.Http2Exception;

import java.util.Arrays;

public class OptionUtil {

    public static final byte[] NO_PREFIX_END = {0};

    public static final ByteSequence prefixEndOf(ByteSequence prefix) {
        byte[] endKey = prefix.getBytes().clone();
        for (int i = endKey.length - 1; i >= 0; i--) {
            if (endKey[i] < 0xff) {
                endKey[i] = (byte) (endKey[i] + 1);
                return ByteSequence.from(Arrays.copyOf(endKey, i + 1));
            }
        }

        return ByteSequence.from(NO_PREFIX_END);
    }

    public static boolean isRecoverable(Status status) {
        return isHaltError(status)
                || isNoLeaderError(status)
                // ephemeral is expired
                || status.getCode() == Status.Code.NOT_FOUND;
    }

    public static boolean isHaltError(Status status) {
        // Unavailable codes mean the system will be right back.
        // (e.g., can't connect, lost leader)
        // Treat Internal codes as if something failed, leaving the
        // system in an inconsistent state, but retrying could make progress.
        // (e.g., failed in middle of send, corrupted frame)
        return status.getCode() != Status.Code.UNAVAILABLE && status.getCode() != Status.Code.INTERNAL;
    }

    public static boolean isNoLeaderError(Status status) {
        return status.getCode() == Status.Code.UNAVAILABLE
                && "etcdserver: no leader".equals(status.getDescription());
    }

    public static boolean isProtocolError(Throwable e) {
        if (e == null) {
            return false;
        }
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof Http2Exception) {
                Http2Exception t = (Http2Exception) cause;
                if ("PROTOCOL_ERROR".equals(t.error().name())) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }
}
