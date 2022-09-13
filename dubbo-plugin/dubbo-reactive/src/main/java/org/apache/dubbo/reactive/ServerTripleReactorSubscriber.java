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

package org.apache.dubbo.reactive;

import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.protocol.tri.CancelableStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.observer.CallStreamObserver;

/**
 * The Subscriber in server to passing the data produced by user publisher to responseStream.
 */
public class ServerTripleReactorSubscriber<T> extends AbstractTripleReactorSubscriber<T> {

    @Override
    public void subscribe(CallStreamObserver<T> downstream) {
        super.subscribe(downstream);
        if (downstream instanceof CancelableStreamObserver) {
            CancelableStreamObserver<?> observer = (CancelableStreamObserver<?>) downstream;
            final CancellationContext context;
            if (observer.getCancellationContext() == null) {
                context = new CancellationContext();
                observer.setCancellationContext(context);
            } else {
                context = observer.getCancellationContext();
            }
            context.addListener(ctx -> super.cancel());
        }
    }
}
