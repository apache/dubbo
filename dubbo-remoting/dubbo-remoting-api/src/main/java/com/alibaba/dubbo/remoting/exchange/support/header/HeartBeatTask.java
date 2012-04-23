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

import java.util.Collection;
import java.util.regex.Pattern;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Client;
import com.alibaba.dubbo.remoting.exchange.Request;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
final class HeartBeatTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger( HeartBeatTask.class );

    private ChannelProvider channelProvider;

    private int             heartbeat;

    private int             heartbeatTimeout;

    public boolean isClient;

    HeartBeatTask( ChannelProvider provider, int heartbeat, int heartbeatTimeout, boolean isClient ) {
        this.channelProvider = provider;
        this.heartbeat = heartbeat;
        this.heartbeatTimeout = heartbeatTimeout;
        this.isClient = isClient;
    }

    public void run() {
        try {
            long now = System.currentTimeMillis();
            for ( Channel channel : channelProvider.getChannels() ) {
                if (channel.isClosed()) {
                    continue;
                }

                String dubboVersion = channel.getUrl().getParameter(
                    Constants.DUBBO_VERSION_KEY, "0.0.0");
                if (isClient && !isSupportHeartbeat(dubboVersion)) {
                    continue;
                }

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
                        logger.warn( "Close channel " + channel
                                             + ", because heartbeat read idle time out." );
                        if (channel instanceof Client) {
                        	try {
                        		((Client)channel).reconnect();
                        	}catch (Exception e) {
								//do nothing
							}
                        } else {
                        	channel.close();
                        }
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

    static final String SUPPORT_HEARTBEAT_VERSION = "2.1.0";
    static final Pattern VERSION_SPLIT_PATTERN = Pattern.compile("\\.");
    static final String SNAPSHOT_SUFFIX = "SNAPSHOT";
    
    static boolean isSupportHeartbeat(String version) {
        String _version = version;
        boolean isSnapshot = false;
        if (_version.endsWith(SNAPSHOT_SUFFIX)) {
            isSnapshot = true;
            _version = _version.substring(0, _version.length() - SNAPSHOT_SUFFIX.length());
            if (_version.endsWith("-")) {
                _version = _version.substring(0, _version.length() - "-".length());
            }
        }
        
        if (SUPPORT_HEARTBEAT_VERSION.equals(_version)) {
            return !isSnapshot;
        }

        String[] parts = VERSION_SPLIT_PATTERN.split(_version);
        int part;
        
        if (parts.length >= 1) {
            part = parseInt(parts[0]);
            if (part > 2) {
                return true;
            }
        }
        
        if (parts.length >= 2) {
            part = parseInt(parts[1]);
            if (part > 1) {
                return true;
            }
        }
        
        if (parts.length >= 3) {
            part = parseInt(parts[2]);
            if (part > 0) {
                return true;
            }
        }
        
        return false;
    }
    
    private static int parseInt(String part) {
        try {
            return Integer.parseInt(part);
        } catch (Throwable e) {
            return -1;
        }
    }
}

