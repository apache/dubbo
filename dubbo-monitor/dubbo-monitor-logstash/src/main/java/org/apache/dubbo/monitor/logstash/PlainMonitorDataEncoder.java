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
package org.apache.dubbo.monitor.logstash;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.monitor.MonitorService;

@ChannelHandler.Sharable
public class PlainMonitorDataEncoder extends MessageToByteEncoder<MonitorData> {
    @Override
    protected void encode(ChannelHandlerContext ctx, MonitorData data, ByteBuf out) throws Exception {
        URL url = data.url
                .addParameters(MonitorService.TIMESTAMP, String.valueOf(data.timestamp),
                        MonitorService.SUCCESS, String.valueOf(data.success),
                        MonitorService.FAILURE, String.valueOf(data.failure),
                        MonitorService.INPUT, String.valueOf(data.input),
                        MonitorService.OUTPUT, String.valueOf(data.output),
                        MonitorService.ELAPSED, String.valueOf(data.elapsed),
                        MonitorService.CONCURRENT, String.valueOf(data.concurrent),
                        MonitorService.MAX_INPUT, String.valueOf(data.maxInput),
                        MonitorService.MAX_OUTPUT, String.valueOf(data.maxOutput),
                        MonitorService.MAX_ELAPSED, String.valueOf(data.maxElapsed),
                        MonitorService.MAX_CONCURRENT, String.valueOf(data.maxConcurrent),
                        Constants.DEFAULT_PROTOCOL, String.valueOf(data.version)
                );
        out.writeBytes(url.toFullString().getBytes());
        out.writeChar('\n');
    }
}
