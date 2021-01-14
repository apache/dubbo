package org.apache.dubbo.remoting;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.netty4.Connection;

import io.netty.channel.group.ChannelGroup;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;

import java.util.Collection;
import java.util.function.Consumer;

@SPI
public interface ConnectionManager {

    Connection connect(URL url) throws RemotingException;

    void forEachConnection(Consumer<Connection> connectionConsumer);

}