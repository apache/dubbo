package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.remoting.api.ConnectionManager;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ReflectionMethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.call.ClientCall;
import org.apache.dubbo.rpc.protocol.tri.support.IGreeter;

import io.netty.channel.Channel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TripleInvokerTest {

    @Test
    public void testNewCall() throws NoSuchMethodException {
        Channel channel = Mockito.mock(Channel.class);
        Connection connection = Mockito.mock(Connection.class);
        ConnectionManager connectionManager = Mockito.mock(ConnectionManager.class);
        when(connectionManager.connect(any(URL.class)))
                .thenReturn(connection);
        when(connection.getChannel())
                .thenReturn(channel);
        URL url = URL.valueOf("tri://127.0.0.1:9103/" + IGreeter.class.getName());
        ExecutorService executorService = url.getOrDefaultApplicationModel().getExtensionLoader(ExecutorRepository.class)
                .getDefaultExtension()
                .createExecutorIfAbsent(url);
        ClientCall call = Mockito.mock(ClientCall.class);
        StreamObserver streamObserver = Mockito.mock(StreamObserver.class);
        when(call.start(any(RequestMetadata.class), any(ClientCall.Listener.class)))
                .thenReturn(streamObserver);
        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName("test");
        TripleInvoker<IGreeter> invoker = new TripleInvoker<>(IGreeter.class, url, connectionManager, new HashSet<>(), executorService);
        MethodDescriptor echoMethod = new ReflectionMethodDescriptor(IGreeter.class.getDeclaredMethod("echo", String.class));
        invoker.invokeUnary(echoMethod, invocation, call);
    }

}