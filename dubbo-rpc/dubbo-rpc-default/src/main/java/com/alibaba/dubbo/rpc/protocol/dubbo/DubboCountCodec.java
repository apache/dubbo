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

package com.alibaba.dubbo.rpc.protocol.dubbo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.io.CountInputStream;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public final class DubboCountCodec implements Codec {

    private static final Logger log = LoggerFactory.getLogger(DubboCountCodec.class);

    private DubboCodec codec = new DubboCodec();

    public void encode(Channel channel, OutputStream output, Object msg) throws IOException {
        codec.encode(channel, output, msg);
    }

    public Object decode(Channel channel, InputStream input) throws IOException {
        CountInputStream statInputStream = new CountInputStream(input);
        Object result = codec.decode(channel, statInputStream);
        if (result != NEED_MORE_INPUT) {
            if (result instanceof Request) {
                try {
                    ((RpcInvocation) ((Request) result).getData()).setAttachment(
                        Constants.INPUT_KEY, String.valueOf(statInputStream.getReadBytes()));
                } catch (Throwable e) {
                    /* ignore */
                }
            } else if (result instanceof Response) {
                try {
                    ((RpcResult) ((Response) result).getResult()).setAttachment(
                        Constants.OUTPUT_KEY, String.valueOf(statInputStream.getReadBytes()));
                } catch (Throwable e) {
                    /* ignreo */
                }
            }
        }
        return result;
    }

}
