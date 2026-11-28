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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.api.connection.ConnectionStateProvider;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.RequestMetadata;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamFactory;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientCallTest {
    @Test
    void streamCompletionReleasesLifecycleResourcesOnceBeforeTheObserver() {
        CancellationContext lifecycle = new CancellationContext();
        AtomicInteger released = new AtomicInteger();
        lifecycle.addListener(ignored -> released.incrementAndGet());
        ClientCall.Listener listener = mock(ClientCall.Listener.class);
        doAnswer(ignored -> {
                    assertEquals(1, released.get());
                    return null;
                })
                .when(listener)
                .onClose(any(), any(), anyBoolean());
        TripleClientCall call = lifecycleCall(lifecycle, listener, mock(ClientStream.class));
        call.onComplete(TriRpcStatus.OK, null, null, false);
        call.onComplete(TriRpcStatus.OK, null, null, false);
        assertEquals(1, released.get());
        assertNull(lifecycle.getCancellationCause());
    }

    @Test
    void nonOkStreamStatusIsVisibleToLifecycleAccountingBeforeObserverDelivery() {
        CancellationContext lifecycle = new CancellationContext();
        ClientCall.Listener listener = mock(ClientCall.Listener.class);
        doAnswer(ignored -> {
                    assertEquals(
                            TriRpcStatus.Code.PERMISSION_DENIED,
                            TriRpcStatus.getStatus(lifecycle.getCancellationCause()).code);
                    return null;
                })
                .when(listener)
                .onClose(any(), any(), anyBoolean());
        ClientStream stream = mock(ClientStream.class);
        TripleClientCall call = lifecycleCall(lifecycle, listener, stream);
        call.onComplete(TriRpcStatus.fromCode(TriRpcStatus.Code.PERMISSION_DENIED), null, null, false);
        verify(listener).onClose(any(), any(), anyBoolean());
        verify(stream, never()).cancelByLocal(any());
    }

    @Test
    void lifecycleCancellationClosesTheTransportEvenWhenRequestedBeforeStart() {
        for (boolean before : new boolean[] {true, false}) {
            CancellationContext lifecycle = new CancellationContext();
            IllegalStateException cause = new IllegalStateException("reference destroyed");
            if (before) lifecycle.cancel(cause);
            ClientStream stream = mock(ClientStream.class);
            TripleClientCall call = lifecycleCall(lifecycle, mock(ClientCall.Listener.class), stream);
            if (!before) lifecycle.cancel(cause);
            verify(stream).cancelByLocal(any());
            assertSame(cause, lifecycle.getCancellationCause());
            assertThrows(IllegalStateException.class, () -> call.sendMessage("late"));
        }
    }

    private static TripleClientCall lifecycleCall(
            CancellationContext context, ClientCall.Listener listener, ClientStream stream) {
        FrameworkModel framework = mock(FrameworkModel.class);
        ClientStreamFactory factory = mock(ClientStreamFactory.class);
        when(framework.getActivateExtensions(ClientStreamFactory.class)).thenReturn(Collections.singletonList(factory));
        when(factory.createClientStream(any(), any(), any(), any(), any())).thenReturn(stream);
        TripleClientCall call = new TripleClientCall(
                mock(AbstractConnectionClient.class), Runnable::run, framework, mock(TripleWriteQueue.class));
        RequestMetadata metadata = new RequestMetadata();
        metadata.streamCancellation = context;
        call.start(metadata, listener);
        return call;
    }

    @Test
    void localCancellationBeforeStartClosesTheLaterAllocatedStream() {
        FrameworkModel framework = mock(FrameworkModel.class);
        ClientStreamFactory factory = mock(ClientStreamFactory.class);
        ClientStream stream = mock(ClientStream.class);
        when(framework.getActivateExtensions(ClientStreamFactory.class)).thenReturn(Collections.singletonList(factory));
        when(factory.createClientStream(any(), any(), any(), any(), any())).thenReturn(stream);
        TripleClientCall call = new TripleClientCall(
                mock(AbstractConnectionClient.class), Runnable::run, framework, mock(TripleWriteQueue.class));
        IllegalStateException cause = new IllegalStateException("deadline before stream start");
        call.cancelByLocal(cause);
        CancellationContext lifecycle = new CancellationContext();
        RequestMetadata metadata = new RequestMetadata();
        metadata.streamCancellation = lifecycle;
        call.start(metadata, mock(ClientCall.Listener.class));
        call.cancelByLocal(cause);
        verify(stream).cancelByLocal(any());
        assertSame(cause, lifecycle.getCancellationCause());
        assertThrows(IllegalStateException.class, () -> call.sendMessage("late"));
    }

    @Test
    void cancellationBeforeFirstRequestHeaderClosesAllocatedStream() {
        FrameworkModel framework = mock(FrameworkModel.class);
        ClientStreamFactory factory = mock(ClientStreamFactory.class);
        ClientStream stream = mock(ClientStream.class);
        when(framework.getActivateExtensions(ClientStreamFactory.class)).thenReturn(Collections.singletonList(factory));
        when(factory.createClientStream(any(), any(), any(), any(), any())).thenReturn(stream);
        TripleClientCall call = new TripleClientCall(
                mock(AbstractConnectionClient.class), Runnable::run, framework, mock(TripleWriteQueue.class));
        call.start(new RequestMetadata(), mock(ClientCall.Listener.class));
        call.cancelByLocal(new IllegalStateException("deadline"));
        verify(stream).cancelByLocal(any());
        assertThrows(IllegalStateException.class, () -> call.sendMessage("late"));
    }

    @Test
    void nativeCallsRetainTheConnectionAfterTheLastInvokerRetires() {
        for (boolean cancel : new boolean[] {false, true}) {
            AbstractConnectionClient connection =
                    mock(AbstractConnectionClient.class, withSettings().useConstructor());
            when(connection.getUrl())
                    .thenReturn(URL.valueOf("tri://127.0.0.1:17070")
                            .putAttribute(ConnectionStateProvider.BACKOFF_ATTRIBUTE, Boolean.TRUE));
            doCallRealMethod().when(connection).increase();
            doCallRealMethod().when(connection).retain();
            connection.increase();
            doCallRealMethod().when(connection).release();
            FrameworkModel framework = mock(FrameworkModel.class);
            ClientStreamFactory factory = mock(ClientStreamFactory.class);
            when(framework.getActivateExtensions(ClientStreamFactory.class))
                    .thenReturn(Collections.singletonList(factory));
            when(factory.createClientStream(any(), any(), any(), any(), any())).thenReturn(mock(ClientStream.class));
            TripleClientCall call =
                    new TripleClientCall(connection, Runnable::run, framework, mock(TripleWriteQueue.class));
            RequestMetadata metadata = new RequestMetadata();
            metadata.streamCancellation = new CancellationContext();
            call.start(metadata, mock(ClientCall.Listener.class));
            connection.release(); // Retire the last Invoker while a dispatched call is still running.
            verify(connection, never()).destroy();
            if (cancel) call.cancelByLocal(TriRpcStatus.CANCELLED.asException());
            else call.onComplete(TriRpcStatus.OK, null, null, false);
            call.cancelByLocal(TriRpcStatus.CANCELLED.asException());
            call.onComplete(TriRpcStatus.OK, null, null, false);
            verify(connection).destroy();
            verify(connection, times(2)).release();
        }
    }

    @Test
    void ordinaryCallsDoNotRetainConnections() {
        AbstractConnectionClient connection =
                mock(AbstractConnectionClient.class, withSettings().useConstructor());
        when(connection.getUrl()).thenReturn(URL.valueOf("tri://127.0.0.1:17070"));
        doCallRealMethod().when(connection).increase();
        connection.increase();
        doCallRealMethod().when(connection).release();
        FrameworkModel framework = mock(FrameworkModel.class);
        ClientStreamFactory factory = mock(ClientStreamFactory.class);
        when(framework.getActivateExtensions(ClientStreamFactory.class)).thenReturn(Collections.singletonList(factory));
        when(factory.createClientStream(any(), any(), any(), any(), any())).thenReturn(mock(ClientStream.class));
        TripleClientCall call =
                new TripleClientCall(connection, Runnable::run, framework, mock(TripleWriteQueue.class));
        call.start(new RequestMetadata(), mock(ClientCall.Listener.class));
        call.onComplete(TriRpcStatus.OK, null, null, false);
        verify(connection, never()).release();
        connection.release();
        verify(connection).destroy();
    }

    @Test
    void failedNativeStreamAllocationReleasesOnlyItsOwnConnectionReference() {
        AbstractConnectionClient connection =
                mock(AbstractConnectionClient.class, withSettings().useConstructor());
        when(connection.getUrl())
                .thenReturn(URL.valueOf("tri://127.0.0.1:17070")
                        .putAttribute(ConnectionStateProvider.BACKOFF_ATTRIBUTE, Boolean.TRUE));
        doCallRealMethod().when(connection).increase();
        doCallRealMethod().when(connection).retain();
        doCallRealMethod().when(connection).release();
        connection.increase();
        FrameworkModel framework = mock(FrameworkModel.class);
        when(framework.getActivateExtensions(ClientStreamFactory.class)).thenReturn(Collections.emptyList());
        TripleClientCall call =
                new TripleClientCall(connection, Runnable::run, framework, mock(TripleWriteQueue.class));
        assertThrows(
                IllegalStateException.class, () -> call.start(new RequestMetadata(), mock(ClientCall.Listener.class)));
        call.cancelByLocal(TriRpcStatus.CANCELLED.asException());
        verify(connection).release();
        verify(connection, never()).destroy();
        connection.release();
        verify(connection).destroy();
    }
}
