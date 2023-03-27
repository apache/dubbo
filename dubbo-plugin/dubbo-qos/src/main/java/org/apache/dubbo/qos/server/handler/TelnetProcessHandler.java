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
package org.apache.dubbo.qos.server.handler;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.command.CommandExecutor;
import org.apache.dubbo.qos.command.DefaultCommandExecutor;
import org.apache.dubbo.qos.command.exception.NoSuchCommandException;
import org.apache.dubbo.qos.command.exception.PermissionDenyException;
import org.apache.dubbo.qos.command.decoder.TelnetCommandDecoder;
import org.apache.dubbo.qos.common.QosConstants;
import org.apache.dubbo.qos.api.QosConfiguration;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_COMMAND_NOT_FOUND;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_UNEXPECTED_EXCEPTION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_PERMISSION_DENY_EXCEPTION;

/**
 * Telnet process handler
 */
public class TelnetProcessHandler extends SimpleChannelInboundHandler<String> {

    private static final ErrorTypeAwareLogger log = LoggerFactory.getErrorTypeAwareLogger(TelnetProcessHandler.class);
    private final CommandExecutor commandExecutor;

    private final QosConfiguration qosConfiguration;

    public TelnetProcessHandler(FrameworkModel frameworkModel, QosConfiguration qosConfiguration) {
        this.commandExecutor = new DefaultCommandExecutor(frameworkModel);
        this.qosConfiguration = qosConfiguration;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {

        if (StringUtils.isBlank(msg)) {
            ctx.writeAndFlush(QosProcessHandler.PROMPT);
        } else {
            CommandContext commandContext = TelnetCommandDecoder.decode(msg);
            commandContext.setQosConfiguration(qosConfiguration);
            commandContext.setRemote(ctx.channel());

            try {
                String result = commandExecutor.execute(commandContext);
                if (StringUtils.isEquals(QosConstants.CLOSE, result)) {
                    ctx.writeAndFlush(getByeLabel()).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.writeAndFlush(result + QosConstants.BR_STR + QosProcessHandler.PROMPT);
                }
            } catch (NoSuchCommandException ex) {
                ctx.writeAndFlush(msg + " :no such command");
                ctx.writeAndFlush(QosConstants.BR_STR + QosProcessHandler.PROMPT);
                log.error(QOS_COMMAND_NOT_FOUND, "", "", "can not found command " + commandContext, ex);
            } catch (PermissionDenyException ex) {
                ctx.writeAndFlush(msg + " :permission deny");
                ctx.writeAndFlush(QosConstants.BR_STR + QosProcessHandler.PROMPT);
                log.error(QOS_PERMISSION_DENY_EXCEPTION, "", "", "permission deny to access command " + commandContext, ex);
            } catch (Exception ex) {
                ctx.writeAndFlush(msg + " :fail to execute commandContext by " + ex.getMessage());
                ctx.writeAndFlush(QosConstants.BR_STR + QosProcessHandler.PROMPT);
                log.error(QOS_UNEXPECTED_EXCEPTION, "", "", "execute commandContext got exception " + commandContext, ex);
            }
        }
    }

    private String getByeLabel() {
        return "BYE!\n";
    }

}
