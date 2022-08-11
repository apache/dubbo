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
import org.apache.dubbo.remoting.buffer.ChannelBuffers;
import org.apache.dubbo.remoting.buffer.HeapChannelBuffer;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.util.CharsetUtil;

import static java.lang.Math.min;


public class TelnetDetector implements ProtocolDetector {

    private FrameworkModel frameworkModel;
    private final int MaxSize = 2048;
    private final ChannelBuffer AytPreface = new HeapChannelBuffer(new byte[]{(byte) 0xff, (byte) 0xf6});

    public void setFrameworkModel(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public Result detect(ChannelBuffer in) {
        if (in.readableBytes() >= MaxSize) {
            return Result.UNRECOGNIZED;
        }
        Result resCommand = CommandDetect(in);
        if (resCommand.equals(Result.RECOGNIZED)){
            return resCommand;
        }
        Result resAyt = TelnetAytDetect(in);
        if (resAyt.equals(Result.RECOGNIZED)) {
            return resAyt;
        }
        if (resAyt.equals(Result.UNRECOGNIZED) && resCommand.equals(Result.UNRECOGNIZED)) {
            return Result.UNRECOGNIZED;
        }
        return Result.NEED_MORE_DATA;
    }

    private Result CommandDetect(ChannelBuffer in) {
        // detect if remote channel send a qos command to server
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

    private Result TelnetAytDetect(ChannelBuffer in) {
        // detect if remote channel send a telnet ayt command to server
        int prefaceLen = AytPreface.readableBytes();
        int bytesRead = min(in.readableBytes(), prefaceLen);
        if(bytesRead == 0 || !ChannelBuffers.prefixEquals(in, AytPreface, bytesRead)) {
            return Result.UNRECOGNIZED;
        }
        if(bytesRead == prefaceLen) {
            // we need to consume preface because it's not a qos command
            // consume and remember to mark, pu server handler reset reader index
            in.readBytes(AytPreface.readableBytes());
            in.markReaderIndex();
            return Result.RECOGNIZED;
        }
        return Result.NEED_MORE_DATA;
    }

}
