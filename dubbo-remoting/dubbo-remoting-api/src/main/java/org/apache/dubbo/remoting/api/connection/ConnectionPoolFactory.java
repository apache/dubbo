package org.apache.dubbo.remoting.api.connection;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.api.ConnectionPool;

@SPI(value = "single", scope = ExtensionScope.FRAMEWORK)
public interface ConnectionPoolFactory<T extends ConnectionPoolEntry> {

    @Adaptive(value = "connectionPool")
    ConnectionPool<T> createPool(URL url);
}
