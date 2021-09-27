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

import org.apache.dubbo.common.stream.StreamObserver;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

public class CancellationContext implements Closeable {

    private ArrayList<ExecutableListener> listeners;
    private Throwable cancellationCause;
    private boolean cancelled;
    private final static ConcurrentMap<StreamObserver<?>, CancellationContext> streamObserverContextMap = new ConcurrentHashMap<>();

    public void addListener(
            final CancellationListener cancellationListener, final Executor executor) {
        addListener(cancellationListener, executor, null);
    }

    public void addListener(
            final CancellationListener cancellationListener) {
        addListener(cancellationListener, Runnable::run, null);
    }

    public void addListener(
            final CancellationListener cancellationListener,
            final RpcServiceContext context) {
        addListener(cancellationListener, Runnable::run, context);
    }

    public void addListener(
            final CancellationListener cancellationListener,
            final Executor executor,
            final RpcServiceContext context) {
        addListenerInternal(new ExecutableListener(executor, cancellationListener, context));
    }

    public synchronized void addListenerInternal(ExecutableListener executableListener) {
        if (isCancelled()) {
            executableListener.deliver();
        } else {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(executableListener);
        }
    }

    public void registerStreamObserver(StreamObserver<?> streamObserver) {
        if (Objects.nonNull(streamObserver)) {
            streamObserverContextMap.put(streamObserver, this);
        }
    }

    public boolean cancel(Throwable cause) {
        boolean triggeredCancel = false;
        synchronized (this) {
            if (!cancelled) {
                cancelled = true;
                this.cancellationCause = cause;
                triggeredCancel = true;
            }
        }
        if (triggeredCancel) {
            notifyAndClearListeners();
        }
        return triggeredCancel;
    }

    public boolean cancel(StreamObserver<?> streamObserver, Throwable cause) {
        CancellationContext cancellationContext = streamObserverContextMap.remove(streamObserver);
        if (Objects.nonNull(cancellationContext)) {
            return cancellationContext.cancel(cause);
        }
        return false;
    }

    private void notifyAndClearListeners() {
        ArrayList<ExecutableListener> tmpListeners;
        synchronized (this) {
            if (listeners == null) {
                return;
            }
            tmpListeners = listeners;
            listeners = null;
        }
        for (ExecutableListener tmpListener : tmpListeners) {
            tmpListener.deliver();
        }
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }

    public List<ExecutableListener> getListeners() {
        return listeners;
    }

    public Throwable getCancellationCause() {
        return cancellationCause;
    }

    @Override
    public void close() throws IOException {
        cancel(null);
        Map<StreamObserver<?>, CancellationContext> copyMap = Collections.synchronizedMap(streamObserverContextMap);
        copyMap.entrySet().stream()
            .filter(entry -> Objects.equals(entry.getValue(), this))
            .map(Map.Entry::getKey)
            .findFirst()
            .ifPresent(streamObserverContextMap::remove);
    }
}
