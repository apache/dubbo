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

import java.util.Arrays;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.triple.TripleWrapper;

public class WrapperRequestCodecObserver implements StreamObserver<TripleWrapper.TripleRequestWrapper> {
    private final String[] signatures;
    private final URL url;
    private final MultipleSerialization multipleSerialization;
    private final StreamObserver<Object[]> delegate;

    public WrapperRequestCodecObserver(String[] signatures, URL url, MultipleSerialization multipleSerialization,
        StreamObserver<Object[]> delegate) {
        this.signatures = signatures;
        this.url = url;
        this.multipleSerialization = multipleSerialization;
        this.delegate = delegate;
    }

    @Override
    public void onNext(TripleWrapper.TripleRequestWrapper req) {
        final String serializeType = req.getSerializeType();
        String[] paramTypes = req.getArgTypesList().toArray(new String[req.getArgsCount()]);
        if (!Arrays.equals(this.signatures, paramTypes)) {
            // todo
            throw new IllegalArgumentException("paramTypes is not ");
        }
        final Object[] arguments = TripleUtil.unwrapReq(url, req, multipleSerialization);
        delegate.onNext(arguments);
    }

    @Override
    public void onError(Throwable throwable) {
        delegate.onError(throwable);
    }

    @Override
    public void onCompleted() {
        delegate.onCompleted();
    }
}
