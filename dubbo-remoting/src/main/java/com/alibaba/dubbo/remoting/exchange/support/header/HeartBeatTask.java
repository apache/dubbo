/*
 * Copyright 1999-2012 Alibaba Group.
 *    
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *    
 *        http://www.apache.org/licenses/LICENSE-2.0
 *    
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.alibaba.dubbo.remoting.exchange.support.header;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.exchange.Request;

import java.util.Collection;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
final class HeartBeatTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger( HeartBeatTask.class );

    private ChannelProvider channelProvider;

    private int             heartbeat;

    private int             heartbeatTimeout;

    HeartBeatTask( ChannelProvider provider, int heartbeat, int heartbeatTimeout ) {
        this.channelProvider = provider;
        this.heartbeat = heartbeat;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public void run() {
        try {
            long now = System.currentTimeMillis();
            for ( Channel channel : channelProvider.getChannels() ) {
                try {
                    Long lastRead = ( Long ) channel.getAttribute(
                            HeaderExchangeHandler.KEY_READ_TIMESTAMP );
                    Long lastWrite = ( Long ) channel.getAttribute(
                            HeaderExchangeHandler.KEY_WRITE_TIMESTAMP );
                    if ( ( lastRead != null && now - lastRead > heartbeat )
                            || ( lastWrite != null && now - lastWrite > heartbeat ) ) {
                        Request req = new Request();
                        req.setVersion( "2.0.0" );
                        req.setTwoWay( true );
                        req.setEvent( Request.HEARTBEAT_EVENT );
                        channel.send( req );
                        if ( logger.isDebugEnabled() ) {
                            logger.debug( "Send heartbeat to remote channel "
                                                  + channel.getRemoteAddress() + "." );
                        }
                    }
                    if ( lastRead != null && now - lastRead > heartbeatTimeout ) {
                        logger.warn( "Close remote channel " + channel.getRemoteAddress()
                                             + ", because heartbeat read idle time out." );
                        channel.close();
                    }
                } catch ( Throwable t ) {
                    logger.warn( "Exception when heartbeat to remote channel " + channel.getRemoteAddress(), t );
                }
            }
        } catch ( Throwable t ) {
            logger.info( "Exception when heartbeat to remote channel(s): ", t );
        }
    }

    interface ChannelProvider {
        Collection<Channel> getChannels();
    }

}

