package com.alibaba.dubbo.qos.server.handler;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.qos.command.CommandContext;
import com.alibaba.dubbo.qos.command.CommandExecutor;
import com.alibaba.dubbo.qos.command.DefaultCommandExecutor;
import com.alibaba.dubbo.qos.command.NoSuchCommandException;
import com.alibaba.dubbo.qos.command.decoder.HttpCommandDecoder;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * <pre>
 * 根据传入的HttpRequest进行解析，分析uri以及参数
 *
 * 如果命令没有找到返回404
 * 如果执行失败500
 * 执行成功则返回内容200
 *
 * Http连接均在执行完毕之后断开
 * </pre>
 *
 * @author weipeng2k 2015年9月1日 下午5:13:56
 */
public class HttpProcessHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(HttpProcessHandler.class);
    private static CommandExecutor commandExecutor = new DefaultCommandExecutor();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        CommandContext commandContext = HttpCommandDecoder.decode(msg);
        if (commandContext == null) { // 如果命令无法构造返回404
            log.warn("can not found commandContext url: " + msg.getUri());
            FullHttpResponse response = http_404();
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            commandContext.setRemote(ctx.channel());
            try {
                String result = commandExecutor.execute(commandContext);
                FullHttpResponse response = http_200(result);
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (NoSuchCommandException ex) {
                log.error("can not find commandContext: " + commandContext, ex);
                FullHttpResponse response = http_404();
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (Exception qosEx) {
                log.error("execute commandContext: " + commandContext + " got exception", qosEx);
                FullHttpResponse response = http_500(qosEx.getMessage());
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private static final FullHttpResponse http_200(String result) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(result.getBytes()));
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private static final FullHttpResponse http_404() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private static final FullHttpResponse http_500(String errorMessage) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR
                , Unpooled.wrappedBuffer(errorMessage.getBytes()));
        HttpHeaders httpHeaders = response.headers();
        httpHeaders.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
        httpHeaders.set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

}
