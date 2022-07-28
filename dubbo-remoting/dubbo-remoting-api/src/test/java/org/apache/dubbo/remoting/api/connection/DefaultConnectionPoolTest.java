package org.apache.dubbo.remoting.api.connection;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.Connection;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class DefaultConnectionPoolTest {

    private final URL url = URL.valueOf("dubbo://localhost:20880");


    private DefaultConnectionPool connectionPool = new DefaultConnectionPool(url);

    @Test
    void acquire() {
        Connection connection = Mockito.mock(Connection.class);
        connection=connection.createConnection();
        DefaultConnectionPoolEntry poolEntry = connectionPool.acquire();
        assertNotNull(poolEntry);

        connectionPool.release(poolEntry);

        DefaultConnectionPoolEntry poolEntry2 = connectionPool.acquire();
        assertEquals(poolEntry, poolEntry2);

    }

    @Test
    void release() {
    }
}