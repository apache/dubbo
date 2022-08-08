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
package org.apache.dubbo.qos.pu;

import org.apache.dubbo.qos.command.BaseCommand;
import org.apache.dubbo.qos.command.CommandContext;
import org.apache.dubbo.qos.command.decoder.TelnetCommandDecoder;
import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.util.CharsetUtil;


public class TelnetDetector implements ProtocolDetector {

    private FrameworkModel frameworkModel;
    private final int MaxSize = 2048;

    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public Result detect(ChannelBuffer in) {
        if (in.readableBytes() >= MaxSize) {
            return Result.UNRECOGNIZED;
        }
        // if no \n is found and in.len() is ok, NEED_MORE_DATA
        ChannelBuffer back = in.copy();
        byte[] backBytes = new byte[back.readableBytes()];
        back.getBytes(back.readerIndex(), backBytes);

        String s = new String(backBytes, CharsetUtil.UTF_8);
        // trim /r/n to let parser work for input
        s = s.trim();
        CommandContext commandContext = TelnetCommandDecoder.decode(s);
        if(frameworkModel.getExtensionLoader(BaseCommand.class).hasExtension(commandContext.getCommandName())){
            return Result.RECOGNIZED;
        }
        return Result.UNRECOGNIZED;
    }
}
