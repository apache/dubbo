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
 * <pre>
 * 接受telnet协议，其实就是用户提交的内容，使用\r\n结尾，当然已经被netty过滤了\r\n了。
 * 经过了StringDecoder相关的Decode过程
 * </pre>
 *
 * @author weipeng2k
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
