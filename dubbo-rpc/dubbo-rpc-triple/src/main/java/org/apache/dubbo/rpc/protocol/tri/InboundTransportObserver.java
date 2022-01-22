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

import org.apache.dubbo.rpc.protocol.tri.GrpcStatus.Code;

public abstract class InboundTransportObserver implements TransportObserver {
    private Metadata headers;
    private Metadata trailers;

    public Metadata getHeaders() {
        return headers;
    }

    public Metadata getTrailers() {
        return trailers;
    }

    @Override
    public void onMetadata(Metadata metadata, boolean endStream) {
        if (headers == null) {
            headers = metadata;
        } else {
            trailers = metadata;
        }
    }

    protected GrpcStatus extractStatusFromMeta(Metadata metadata) {
        if (!metadata.contains(TripleHeaderEnum.STATUS_KEY.getHeader())) {
            return GrpcStatus.fromCode(Code.OK);
        }
        final int code = Integer.parseInt(metadata.get(TripleHeaderEnum.STATUS_KEY.getHeader()).toString());

        if (Code.isOk(code)) {
            return GrpcStatus.fromCode(Code.OK);
        }
        GrpcStatus status = GrpcStatus.fromCode(code);
        if (!metadata.contains(TripleHeaderEnum.MESSAGE_KEY.getHeader())) {
            return status;
        }
        final String raw = metadata.get(TripleHeaderEnum.MESSAGE_KEY.getHeader()).toString();
        status = status.withDescription(GrpcStatus.decodeMessage(raw));
        return status;
    }
}
