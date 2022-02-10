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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.rpc.protocol.tri.pack.GenericUnpack;
import org.apache.dubbo.rpc.protocol.tri.pack.WrapRequestUnpack;
import org.apache.dubbo.triple.TripleWrapper;

public class WrapRequestServerCallListener   implements ServerCall.Listener{
    private final ServerCall.Listener delegate;
    private final WrapRequestUnpack unpack;

    public WrapRequestServerCallListener(ServerCall.Listener delegate, GenericUnpack unpack) {
        this.delegate=delegate;
        this.unpack=new WrapRequestUnpack(unpack);
    }

    @Override
    public void onMessage(Object message) {
        final Object args= this.unpack.unpack((TripleWrapper.TripleRequestWrapper) message);
        delegate.onMessage(args);
    }

    @Override
    public void onHalfClose() {
        delegate.onHalfClose();
    }

    @Override
    public void onCancel(String errorInfo) {
        delegate.onCancel(errorInfo);

    }

    @Override
    public void onComplete() {
        delegate.onComplete();
    }
}
