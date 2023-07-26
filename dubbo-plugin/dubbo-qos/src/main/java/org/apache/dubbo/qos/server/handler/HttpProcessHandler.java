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
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.command.CommandExecutor;
import org.apache.dubbo.qos.command.DefaultCommandExecutor;
import org.apache.dubbo.qos.command.exception.NoSuchCommandException;
import org.apache.dubbo.qos.command.exception.PermissionDenyException;
import org.apache.dubbo.qos.command.decoder.HttpCommandDecoder;
import org.apache.dubbo.qos.api.QosConfiguration;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_COMMAND_NOT_FOUND;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_UNEXPECTED_EXCEPTION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.QOS_PERMISSION_DENY_EXCEPTION;

/**
 * Parse HttpRequest for uri and parameters
 * <p>
 * <ul>
 * <li>if command not found, return 404</li>
 * <li>if execution fails, return 500</li>
 * <li>if succeed, return 200</li>
 * </ul>
 * <p>
 * will disconnect after execution finishes
 */
public class HttpProcessHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final ErrorTypeAwareLogger log = LoggerFactory.getErrorTypeAwareLogger(HttpProcessHandler.class);
    private final CommandExecutor commandExecutor;

    private final QosConfiguration qosConfiguration;

    public HttpProcessHandler(FrameworkModel frameworkModel, QosConfiguration qosConfiguration) {
        this.commandExecutor = new DefaultCommandExecutor(frameworkModel);
        this.qosConfiguration = qosConfiguration;
    }

    private static FullHttpResponse http(int httpCode, String result) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(httpCode)
            , Unpooled.wrappedBuffer(result.getBytes()));
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private static FullHttpResponse http(int httpCode) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(httpCode));
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        CommandContext commandContext = HttpCommandDecoder.decode(msg);
        // return 404 when fail to construct command context
        if (commandContext == null) {
            log.warn(QOS_UNEXPECTED_EXCEPTION, "", "", "can not found commandContext, url: " + msg.uri());
            FullHttpResponse response = http(404);
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            commandContext.setRemote(ctx.channel());
            commandContext.setQosConfiguration(qosConfiguration);
            try {
                String result = commandExecutor.execute(commandContext);
                int httpCode = commandContext.getHttpCode();
                FullHttpResponse response = http(httpCode, result);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (NoSuchCommandException ex) {
                log.error(QOS_COMMAND_NOT_FOUND, "", "", "can not find command: " + commandContext, ex);
                FullHttpResponse response = http(404);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (PermissionDenyException ex) {
                log.error(QOS_PERMISSION_DENY_EXCEPTION, "", "", "permission deny to access command: " + commandContext, ex);
                FullHttpResponse response = http(403);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (Exception qosEx) {
                log.error(QOS_UNEXPECTED_EXCEPTION, "", "", "execute commandContext: " + commandContext + " got exception", qosEx);
                FullHttpResponse response = http(500, qosEx.getMessage());
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

}
