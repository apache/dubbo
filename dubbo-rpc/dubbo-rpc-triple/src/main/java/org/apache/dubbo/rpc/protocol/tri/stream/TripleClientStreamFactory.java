package org.apache.dubbo.rpc.protocol.tri.stream;

import io.netty.channel.Channel;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.transport.TripleWriteQueue;

import java.util.concurrent.Executor;

@SPI(scope = ExtensionScope.FRAMEWORK)
public interface TripleClientStreamFactory {
    ClientStream create(
            FrameworkModel frameworkModel,
            Executor executor,
            Channel parent,
            ClientStream.Listener listener,
            TripleWriteQueue writeQueue);
    boolean supportConnectionType(AbstractConnectionClient connectionClient);
}
