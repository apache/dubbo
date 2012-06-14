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
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;
import com.alibaba.dubbo.remoting.transport.dispather.all.AllDispather;

/**
 * ChannelHandlerWrapper (SPI, Singleton, ThreadSafe)
 * 
 * @author chao.liuc
 */
@SPI(AllDispather.NAME)
public interface Dispather {

    /**
     * dispath.
     * 
     * @param handler
     * @param url
     * @return channel handler
     */
    @Adaptive({Constants.DISPATHER_KEY, Constants.CHANNEL_HANDLER_KEY})
    ChannelHandler dispath(ChannelHandler handler, URL url);

}