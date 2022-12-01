package org.apache.dubbo.remoting.transport.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;


public class HttpProcessHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(HttpProcessHandler.class);


  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {


  }

}
