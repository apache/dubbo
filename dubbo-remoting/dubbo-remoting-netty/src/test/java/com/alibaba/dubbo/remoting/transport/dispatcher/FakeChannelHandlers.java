/*
 * Copyright 1999-2011 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.remoting.transport.dispatcher;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Dispatcher;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class FakeChannelHandlers extends ChannelHandlers {

    public FakeChannelHandlers() {
        super();
    }

    public static void setTestingChannelHandlers() {
        ChannelHandlers.setTestingChannelHandlers(new FakeChannelHandlers());
    }

    public static void resetChannelHandlers() {
        ChannelHandlers.setTestingChannelHandlers(new ChannelHandlers());
    }

    @Override
    protected ChannelHandler wrapInternal(ChannelHandler handler, URL url) {
        return ExtensionLoader.getExtensionLoader(Dispatcher.class)
                .getAdaptiveExtension().dispatch(handler, url);
    }
}
