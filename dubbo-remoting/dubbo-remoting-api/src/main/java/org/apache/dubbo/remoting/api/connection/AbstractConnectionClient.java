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
package org.apache.dubbo.remoting.api.connection;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.transport.AbstractClient;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;

public abstract class AbstractConnectionClient extends AbstractClient {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(AbstractConnectionClient.class);

    protected WireProtocol protocol;

    protected InetSocketAddress remote;

    protected AtomicBoolean init;

    protected static final Object CONNECTED_OBJECT = new Object();

    private volatile long counter;

    private static final AtomicLongFieldUpdater<AbstractConnectionClient> COUNTER_UPDATER
            = AtomicLongFieldUpdater.newUpdater(AbstractConnectionClient.class, "counter");

    protected AbstractConnectionClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        COUNTER_UPDATER.set(this, 1L);
    }

    /**
     * Increments the reference count by 1.
     */
    public final AbstractConnectionClient retain() {
        long oldCount = COUNTER_UPDATER.getAndIncrement(this);
        if (oldCount <= 0) {
            COUNTER_UPDATER.getAndDecrement(this);
            throw new AssertionError("This instance has been destroyed");
        }
        return this;
    }

    /**
     * Decreases the reference count by 1 and calls {@link this#destroy} if the reference count reaches 0.
     */
    public final boolean release() {
        long remainingCount = COUNTER_UPDATER.decrementAndGet(this);

        if (remainingCount == 0) {
            destroy();
            return true;
        } else if (remainingCount <= -1) {
            logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", "This instance has been destroyed");
            return false;
        } else {
            return false;
        }
    }

    /**
     * init config and attribute.
     */
    protected abstract void initConnectionClient();

    /**
     * connection is available.
     *
     * @return boolean
     */
    public abstract boolean isAvailable();

    /**
     * create connecting promise.
     */
    public abstract void createConnectingPromise();

    /**
     * add the listener of close connection event.
     *
     * @param func execute function
     */
    public abstract void addCloseListener(Runnable func);

    /**
     * when connected, callback.
     *
     * @param channel Channel
     */
    public abstract void onConnected(Object channel);

    /**
     * when goaway, callback.
     *
     * @param channel Channel
     */
    public abstract void onGoaway(Object channel);

    /**
     * This method will be invoked when counter reaches 0, override this method to destroy materials related to the specific resource.
     */
    public abstract void destroy();

    /**
     * if generalizable, return NIOChannel
     * else return Dubbo Channel
     *
     * @param generalizable generalizable
     * @return Dubbo Channel or NIOChannel such as NettyChannel
     */
    public abstract Object getChannel(Boolean generalizable);

    /**
     * Get counter
     */
    public long getCounter() {
        return COUNTER_UPDATER.get(this);
    }

}
