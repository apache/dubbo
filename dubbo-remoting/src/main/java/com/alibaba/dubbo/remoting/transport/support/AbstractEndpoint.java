/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.transport.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.Resetable;

/**
 * AbstractEndpoint
 * 
 * @author william.liangf
 */
public abstract class AbstractEndpoint extends AbstractPeer implements Resetable {

    private Codec                 codec;

    private int                   timeout;

    private int                   connectTimeout;
    
    public AbstractEndpoint(URL url, ChannelHandler handler) {
        super(url, handler);
        this.codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(url.getParameter(Constants.CODEC_KEY, "telnet"));
        this.timeout = url.getPositiveIntParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        this.connectTimeout = url.getPositiveIntParameter(Constants.CONNECT_TIMEOUT_KEY, timeout);
    }

    public void reset(URL url) {
        if (isClosed()) {
            throw new IllegalStateException("Failed to reset parameters "
                                        + url + ", cause: Channel closed. channel: " + getLocalAddress());
        }
        try {
            if (url.hasParameter(Constants.HEARTBEAT_KEY)) {
                int t = url.getIntParameter(Constants.TIMEOUT_KEY);
                if (t > 0) {
                    this.timeout = t;
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        try {
            if (url.hasParameter(Constants.CONNECT_TIMEOUT_KEY)) {
                int t = url.getIntParameter(Constants.CONNECT_TIMEOUT_KEY);
                if (t > 0) {
                    this.connectTimeout = t;
                }
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        try {
            if (url.hasParameter(Constants.CODEC_KEY)) {
                String c = url.getParameter(Constants.CODEC_KEY);
                this.codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(c);
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    protected Codec getCodec() {
        return codec;
    }

    protected int getTimeout() {
        return timeout;
    }

    protected int getConnectTimeout() {
        return connectTimeout;
    }

}