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
package com.alibaba.dubbo.qos.server.handler;


import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.CommandExecutor;
import com.alibaba.dubbo.qos.command.DefaultCommandExecutor;
import com.alibaba.dubbo.qos.command.NoSuchCommandException;
import com.alibaba.dubbo.qos.command.decoder.TelnetCommandDecoder;
import com.alibaba.dubbo.qos.common.Constants;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.lang3.StringUtils;

/**
 * Telnet process handler
 */
public class TelnetProcessHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(TelnetProcessHandler.class);
    private static CommandExecutor commandExecutor = new DefaultCommandExecutor();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        if (StringUtils.isBlank(msg)) {
            ctx.writeAndFlush(QosProcessHandler.prompt);
        } else {
            CommandContext commandContext = TelnetCommandDecoder.decode(msg);
            commandContext.setRemote(ctx.channel());

            try {
                String result = commandExecutor.execute(commandContext);
                if (StringUtils.equals(Constants.CLOSE, result)) {
                    ctx.writeAndFlush(getByeLabel()).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.writeAndFlush(result + Constants.BR_STR + QosProcessHandler.prompt);
                }
            } catch (NoSuchCommandException ex) {
                ctx.writeAndFlush(msg + " :no such command");
                ctx.writeAndFlush(Constants.BR_STR + QosProcessHandler.prompt);
                log.error("can not found command " + commandContext, ex);
            } catch (Exception ex) {
                ctx.writeAndFlush(msg + " :fail to execute commandContext by " + ex.getMessage());
                ctx.writeAndFlush(Constants.BR_STR + QosProcessHandler.prompt);
                log.error("execute commandContext got exception " + commandContext, ex);
            }
        }
    }

    private String getByeLabel() {
        return "BYE!\n";
    }

}
