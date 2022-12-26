package org.apache.dubbo.remoting.http;

import org.apache.dubbo.remoting.RemotingException;

import java.io.IOException;


public interface RestClient<REQ, RES> {
    /**
     * send message.
     *
     * @param message
     * @throws RemotingException
     */
    RES send(REQ message) throws IOException;

    /**
     * close the channel.
     */
    void close();

    /**
     * Graceful close the channel.
     */
    void close(int timeout);

    /**
     * is closed.
     *
     * @return closed
     */
    boolean isClosed();

}
