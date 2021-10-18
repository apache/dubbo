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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.rpc.CancellationContext;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CancelableStreamObserver<T> implements StreamObserver<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CancelableStreamObserver.class);
    private final AtomicBoolean contextSet = new AtomicBoolean(false);
    private CancellationContext cancellationContext;

    public CancelableStreamObserver() {
    }

    public CancelableStreamObserver(CancellationContext cancellationContext) {
        setCancellationContext(cancellationContext);
    }

    public final void setCancellationContext(CancellationContext cancellationContext) {
        if (contextSet.compareAndSet(false, true)) {
            this.cancellationContext = cancellationContext;
        } else {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("CancellationContext already set,do not repeat the set, ignore this set");
            }
        }
    }

    public final void cancel(Throwable throwable) {
        if (cancellationContext == null) {
            return;
        }
        cancellationContext.cancel(throwable);
    }
}
