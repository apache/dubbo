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
package com.alibaba.dubbo.remoting.p2p;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;

/**
 * Networkers. (API, Static, ThreadSafe)
 * 
 * <a href="http://en.wikipedia.org/wiki/Peer-to-peer">Peer-to-peer</a>
 * 
 * @author william.liangf
 */
public class Networkers {
    
    public static Peer join(String group, String peer, ChannelHandler handler) throws RemotingException {
        return join(URL.valueOf(group), URL.valueOf(peer), handler);
    }

    public static Peer join(URL group, URL peer, ChannelHandler handler) throws RemotingException {
        return lookup(group).join(peer, handler);
    }
    
    public static Group lookup(String group) throws RemotingException {
        return lookup(URL.valueOf(group));
    }
    
    public static Group lookup(URL group) throws RemotingException {
        Networker networker = ExtensionLoader.getExtensionLoader(Networker.class).getExtension(group.getProtocol());
        return networker.lookup(group);
    }
}