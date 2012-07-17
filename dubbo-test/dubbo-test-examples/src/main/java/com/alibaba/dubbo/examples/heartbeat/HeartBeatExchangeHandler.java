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

package com.alibaba.dubbo.examples.heartbeat;

import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeHandler;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class HeartBeatExchangeHandler extends HeaderExchangeHandler {

    private AtomicInteger heartBeatCounter = new AtomicInteger( 0 );
    
    public HeartBeatExchangeHandler( ExchangeHandler handler ) {
        super( handler );
    }

    @Override
    public void received( Channel channel, Object message ) throws RemotingException {
        if ( message instanceof Request ) {
            Request req = ( Request ) message;
            if ( req.isHeartbeat() ) {
                heartBeatCounter.incrementAndGet();
                channel.setAttribute(KEY_READ_TIMESTAMP, System.currentTimeMillis());
                Response res = new Response( req.getId(), req.getVersion() );
                res.setEvent( req.getData() == null ? null : req.getData().toString() );
                channel.send( res );
            }
        } else {
            super.received( channel, message );
        }
    }
    
    public int getHeartBeatCount() {
        return heartBeatCounter.get();
    }
    
}
