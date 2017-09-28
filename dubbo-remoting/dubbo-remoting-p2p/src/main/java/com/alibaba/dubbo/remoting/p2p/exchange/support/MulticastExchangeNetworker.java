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
package com.alibaba.dubbo.remoting.p2p.exchange.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.p2p.exchange.ExchangeGroup;
import com.alibaba.dubbo.remoting.p2p.exchange.ExchangeNetworker;

/**
 * MulticastNetworker
 *
 * @author william.liangf
 */
public class MulticastExchangeNetworker implements ExchangeNetworker {

    public ExchangeGroup lookup(URL url) throws RemotingException {
        return new MulticastExchangeGroup(url);
    }

}