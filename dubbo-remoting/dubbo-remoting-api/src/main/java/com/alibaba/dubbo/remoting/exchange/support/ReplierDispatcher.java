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
package com.alibaba.dubbo.remoting.exchange.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;

/**
 * ReplierDispatcher
 * 
 * @author william.liangf
 */
public class ReplierDispatcher implements Replier<Object> {

    private final Replier<?> defaultReplier;
    
    private final Map<Class<?>, Replier<?>> repliers = new ConcurrentHashMap<Class<?>, Replier<?>>();

    public ReplierDispatcher(){
        this(null, null);
    }
    
    public ReplierDispatcher(Replier<?> defaultReplier){
        this(defaultReplier, null);
    }

    public ReplierDispatcher(Replier<?> defaultReplier, Map<Class<?>, Replier<?>> repliers){
        this.defaultReplier = defaultReplier;
        if (repliers != null && repliers.size() > 0) {
            this.repliers.putAll(repliers);
        }
    }

    public <T> ReplierDispatcher addReplier(Class<T> type, Replier<T> replier) {
        repliers.put(type, replier);
        return this;
    }

    public <T> ReplierDispatcher removeReplier(Class<T> type) {
        repliers.remove(type);
        return this;
    }

    private Replier<?> getReplier(Class<?> type) {
        for(Map.Entry<Class<?>, Replier<?>> entry : repliers.entrySet()) {
            if(entry.getKey().isAssignableFrom(type)) {
                return entry.getValue();
            }
        }
        if (defaultReplier != null) {
            return defaultReplier;
        }
        throw new IllegalStateException("Replier not found, Unsupported message object: " + type);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
        return ((Replier)getReplier(request.getClass())).reply(channel, request);
    }

}