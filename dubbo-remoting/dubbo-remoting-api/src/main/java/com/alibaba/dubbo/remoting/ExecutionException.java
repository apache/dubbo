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

import java.net.InetSocketAddress;

/**
 * ReceiveException
 *
 * @author william.liangf
 * @export
 */
public class ExecutionException extends RemotingException {

    private static final long serialVersionUID = -2531085236111056860L;

    private final Object request;

    public ExecutionException(Object request, Channel channel, String message, Throwable cause) {
        super(channel, message, cause);
        this.request = request;
    }

    public ExecutionException(Object request, Channel channel, String msg) {
        super(channel, msg);
        this.request = request;
    }

    public ExecutionException(Object request, Channel channel, Throwable cause) {
        super(channel, cause);
        this.request = request;
    }

    public ExecutionException(Object request, InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message,
                              Throwable cause) {
        super(localAddress, remoteAddress, message, cause);
        this.request = request;
    }

    public ExecutionException(Object request, InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message) {
        super(localAddress, remoteAddress, message);
        this.request = request;
    }

    public ExecutionException(Object request, InetSocketAddress localAddress, InetSocketAddress remoteAddress, Throwable cause) {
        super(localAddress, remoteAddress, cause);
        this.request = request;
    }


    public Object getRequest() {
        return request;
    }

}