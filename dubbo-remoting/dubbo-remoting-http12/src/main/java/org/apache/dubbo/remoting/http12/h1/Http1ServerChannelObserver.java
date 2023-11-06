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
package org.apache.dubbo.remoting.http12.h1;

import org.apache.dubbo.remoting.http12.AbstractServerHttpChannelObserver;
import org.apache.dubbo.remoting.http12.HttpChannel;
import org.apache.dubbo.remoting.http12.HttpChannelObserver;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.HttpOutputMessage;

public class Http1ServerChannelObserver extends AbstractServerHttpChannelObserver
        implements HttpChannelObserver<Object> {

    public Http1ServerChannelObserver(HttpChannel httpChannel) {
        super(httpChannel);
    }

    @Override
    protected HttpMetadata encodeHttpMetadata() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaderNames.TRANSFER_ENCODING.getName(), "chunked");
        return new Http1Metadata(httpHeaders);
    }

    @Override
    protected void doOnCompleted(Throwable throwable) {
        super.doOnCompleted(throwable);
        this.getHttpChannel().writeMessage(HttpOutputMessage.EMPTY_MESSAGE);
    }
}
