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
package org.apache.dubbo.remoting.http12.h2;

import org.apache.dubbo.remoting.http12.AbstractServerTransportListener;
import org.apache.dubbo.remoting.http12.HttpChannelObserver;
import org.apache.dubbo.rpc.model.FrameworkModel;

/**
 * @author icodening
 * @date 2023.06.13
 */
public class GenericHttp2ServerTransportListener extends AbstractServerTransportListener<Http2Header, Http2Message> implements Http2ServerTransportListener {

    public GenericHttp2ServerTransportListener(H2StreamChannel h2StreamChannel, FrameworkModel frameworkModel) {
        super(h2StreamChannel, frameworkModel);
    }

    @Override
    public void cancelByRemote(long errorCode) {

    }

    @Override
    protected H2StreamChannel getHttpChannel() {
        return (H2StreamChannel) super.getHttpChannel();
    }

    @Override
    protected HttpChannelObserver createHttpChannelObserver() {
        return new Http2ChannelObserver(getHttpChannel(), getCodec());
    }
}
