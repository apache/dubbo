package org.apache.dubbo.remoting.api.connection;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.ConnectionPool;

public class DefaultConnectionPoolFactory implements ConnectionPoolFactory<SingleConnectionPoolEntry> {

    @Override
    public ConnectionPool<SingleConnectionPoolEntry> createPool(URL url) {
        return new SingleConnectionPool(url);
    }
}
