package org.apache.dubbo.rpc.protocol.tri.stream;

import io.netty.channel.Channel;

import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream.Listener;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.concurrent.Executor;

public class TripleHttp12ClientStreamFactory implements TripleClientStreamFactory {
    @Override
    public ClientStream create(
            FrameworkModel frameworkModel,
            Executor executor,
            Channel parent,
            Listener listener,
            TripleWriteQueue writeQueue) {
        return new TripleClientStream(frameworkModel, executor, parent, listener, writeQueue);
    }

    @Override
    public boolean supportConnectionType(AbstractConnectionClient connectionClient) {
        return connectionClient.getConnectionType().equalsIgnoreCase("http12");
    }
}
