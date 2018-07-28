package org.apache.dubbo.qos.command.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.dubbo.qos.command.CommandContext;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpCommandDecoderTest {
    @Test
    public void decodeGet() throws Exception {
        HttpRequest request = mock(HttpRequest.class);
        when(request.getUri()).thenReturn("localhost:80/test");
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        CommandContext context = HttpCommandDecoder.decode(request);
        assertThat(context.getCommandName(), equalTo("test"));
        assertThat(context.isHttp(), is(true));
        when(request.getUri()).thenReturn("localhost:80/test?a=b&c=d");
        context = HttpCommandDecoder.decode(request);
        assertThat(context.getArgs(), arrayContaining("b", "d"));
    }

    @Test
    public void decodePost() throws Exception {
        FullHttpRequest request = mock(FullHttpRequest.class);
        when(request.getUri()).thenReturn("localhost:80/test");
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.headers()).thenReturn(HttpHeaders.EMPTY_HEADERS);
        ByteBuf buf = Unpooled.copiedBuffer("a=b&c=d", StandardCharsets.UTF_8);
        when(request.content()).thenReturn(buf);
        CommandContext context = HttpCommandDecoder.decode(request);
        assertThat(context.getCommandName(), equalTo("test"));
        assertThat(context.isHttp(), is(true));
        assertThat(context.getArgs(), arrayContaining("b", "d"));
    }
}
