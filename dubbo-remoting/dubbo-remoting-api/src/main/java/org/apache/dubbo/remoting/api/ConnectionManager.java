package org.apache.dubbo.remoting.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.Connection;

import java.util.function.Consumer;

@SPI
public interface ConnectionManager {

    Connection connect(URL url) throws RemotingException;

    void forEachConnection(Consumer<Connection> connectionConsumer);

}