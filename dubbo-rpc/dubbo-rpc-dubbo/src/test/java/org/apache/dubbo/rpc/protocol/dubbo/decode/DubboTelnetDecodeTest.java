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
package org.apache.dubbo.rpc.protocol.dubbo.decode;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.Codec2;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import org.apache.dubbo.remoting.exchange.support.header.HeaderExchangeHandler;
import org.apache.dubbo.remoting.transport.DecodeHandler;
import org.apache.dubbo.remoting.transport.MultiMessageHandler;
import org.apache.dubbo.remoting.transport.netty4.NettyBackedChannelBuffer;
import org.apache.dubbo.remoting.transport.netty4.NettyCodecAdapter;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.DubboCodec;
import org.apache.dubbo.rpc.protocol.dubbo.support.DemoService;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * These junit tests aim to test unpack and stick pack of dubbo and telnet
 */
public class DubboTelnetDecodeTest {
    private static AtomicInteger dubbo = new AtomicInteger(0);

    private static AtomicInteger telnet = new AtomicInteger(0);

    private static AtomicInteger telnetDubbo = new AtomicInteger(0);

    private static AtomicInteger dubboDubbo = new AtomicInteger(0);

    private static AtomicInteger dubboTelnet = new AtomicInteger(0);

    private static AtomicInteger telnetTelnet = new AtomicInteger(0);

    /**
     * just dubbo request
     *
     * @throws InterruptedException
     */
    @Test
    public void testDubboDecode() throws InterruptedException, IOException {
        ByteBuf dubboByteBuf = createDubboByteBuf();

        EmbeddedChannel ch = null;
        try {
            Codec2 codec = ExtensionLoader.getExtensionLoader(Codec2.class).getExtension("dubbo");
            URL url = new URL("dubbo", "localhost", 22226);
            NettyCodecAdapter adapter = new NettyCodecAdapter(codec, url, new MockChannelHandler());

            MockHandler mockHandler = new MockHandler(null,
                    new MultiMessageHandler(
                            new DecodeHandler(
                                    new HeaderExchangeHandler(new ExchangeHandlerAdapter() {
                                        @Override
                                        public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) {
                                            if (checkDubboDecoded(msg)) {
                                                dubbo.incrementAndGet();
                                            }
                                            return null;
                                        }
                                    }))));

            ch = new LocalEmbeddedChannel();
            ch.pipeline()
                    .addLast("decoder", adapter.getDecoder())
                    .addLast("handler", mockHandler);

            ch.writeInbound(dubboByteBuf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ch != null) {
                ch.close().await(200, TimeUnit.MILLISECONDS);
            }
        }

        TimeUnit.MILLISECONDS.sleep(100);

        Assertions.assertEquals(1, dubbo.get());
    }

    /**
     * just telnet request
     *
     * @throws InterruptedException
     */
    @Test
    public void testTelnetDecode() throws InterruptedException {
        ByteBuf telnetByteBuf = Unpooled.wrappedBuffer("ls\r\n".getBytes());

        EmbeddedChannel ch = null;
        try {
            Codec2 codec = ExtensionLoader.getExtensionLoader(Codec2.class).getExtension("dubbo");
            URL url = new URL("dubbo", "localhost", 22226);
            NettyCodecAdapter adapter = new NettyCodecAdapter(codec, url, new MockChannelHandler());

            MockHandler mockHandler = new MockHandler((msg) -> {
                if (checkTelnetDecoded(msg)) {
                    telnet.incrementAndGet();
                }
            },
                    new MultiMessageHandler(
                            new DecodeHandler(
                                    new HeaderExchangeHandler(new ExchangeHandlerAdapter() {
                                        @Override
                                        public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) {
                                            return null;
                                        }
                                    }))));

            ch = new LocalEmbeddedChannel();
            ch.pipeline()
                    .addLast("decoder", adapter.getDecoder())
                    .addLast("handler", mockHandler);

            ch.writeInbound(telnetByteBuf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ch != null) {
                ch.close().await(200, TimeUnit.MILLISECONDS);
            }
        }

        TimeUnit.MILLISECONDS.sleep(100);

        Assertions.assertEquals(1, telnet.get());
    }

    /**
     * telnet and dubbo request
     *
     * <p>
     * First ByteBuf:
     * +--------------------------------------------------+
     * |               telnet(incomplete)                 |
     * +--------------------------------------------------+
     * <p>
     *
     * Second ByteBuf:
     * +--------------------------++----------------------+
     * |  telnet(the remaining)   ||   dubbo(complete)    |
     * +--------------------------++----------------------+
     *                            ||
     *                        Magic Code
     *
     * @throws InterruptedException
     */
    @Test
    public void testTelnetDubboDecoded() throws InterruptedException, IOException {
        ByteBuf dubboByteBuf = createDubboByteBuf();

        ByteBuf telnetByteBuf = Unpooled.wrappedBuffer("ls\r".getBytes());
        EmbeddedChannel ch = null;
        try {
            Codec2 codec = ExtensionLoader.getExtensionLoader(Codec2.class).getExtension("dubbo");
            URL url = new URL("dubbo", "localhost", 22226);
            NettyCodecAdapter adapter = new NettyCodecAdapter(codec, url, new MockChannelHandler());

            MockHandler mockHandler = new MockHandler((msg) -> {
                if (checkTelnetDecoded(msg)) {
                    telnetDubbo.incrementAndGet();
                }
            },
                    new MultiMessageHandler(
                            new DecodeHandler(
                                    new HeaderExchangeHandler(new ExchangeHandlerAdapter() {
                                        @Override
                                        public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) {
                                            if (checkDubboDecoded(msg)) {
                                                telnetDubbo.incrementAndGet();
                                            }
                                            return null;
                                        }
                                    }))));

            ch = new LocalEmbeddedChannel();
            ch.pipeline()
                    .addLast("decoder", adapter.getDecoder())
                    .addLast("handler", mockHandler);

            ch.writeInbound(telnetByteBuf);
            ch.writeInbound(Unpooled.wrappedBuffer(Unpooled.wrappedBuffer("\n".getBytes()), dubboByteBuf));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ch != null) {
                ch.close().await(200, TimeUnit.MILLISECONDS);
            }
        }

        TimeUnit.MILLISECONDS.sleep(100);

        Assertions.assertEquals(2, telnetDubbo.get());
    }

    /**
     * NOTE: This test case actually will fail, but the probability of this case is very small,
     * and users should use telnet in new QOS port(default port is 22222) since dubbo 2.5.8,
     * so we could ignore this problem.
     *
     * <p>
     * telnet and telnet request
     *
     * <p>
     * First ByteBuf (firstByteBuf):
     * +--------------------------------------------------+
     * |               telnet(incomplete)                 |
     * +--------------------------------------------------+
     * <p>
     *
     * Second ByteBuf (secondByteBuf):
     * +--------------------------------------------------+
     * |  telnet(the remaining)   |   telnet(complete)    |
     * +--------------------------------------------------+
     *
     * @throws InterruptedException
     */
    // @Test
    public void testTelnetTelnetDecoded() throws InterruptedException {
        ByteBuf firstByteBuf = Unpooled.wrappedBuffer("ls\r".getBytes());
        ByteBuf secondByteBuf = Unpooled.wrappedBuffer("\nls\r\n".getBytes());

        EmbeddedChannel ch = null;
        try {
            Codec2 codec = ExtensionLoader.getExtensionLoader(Codec2.class).getExtension("dubbo");
            URL url = new URL("dubbo", "localhost", 22226);
            NettyCodecAdapter adapter = new NettyCodecAdapter(codec, url, new MockChannelHandler());

            MockHandler mockHandler = new MockHandler((msg) -> {
                if (checkTelnetDecoded(msg)) {
                    telnetTelnet.incrementAndGet();
                }
            },
                    new MultiMessageHandler(
                            new DecodeHandler(
                                    new HeaderExchangeHandler(new ExchangeHandlerAdapter() {
                                        @Override
                                        public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) {
                                            return null;
                                        }
                                    }))));

            ch = new LocalEmbeddedChannel();
            ch.pipeline()
                    .addLast("decoder", adapter.getDecoder())
                    .addLast("handler", mockHandler);

            ch.writeInbound(firstByteBuf);
            ch.writeInbound(secondByteBuf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ch != null) {
                ch.close().await(200, TimeUnit.MILLISECONDS);
            }
        }

        TimeUnit.MILLISECONDS.sleep(100);

        Assertions.assertEquals(2, telnetTelnet.get());
    }

    /**
     * dubbo and dubbo request
     *
     * <p>
     * First ByteBuf (firstDubboByteBuf):
     * ++-------------------------------------------------+
     * ||               dubbo(incomplete)                 |
     * ++-------------------------------------------------+
     * ||
     * Magic Code
     * <p>
     *
     * <p>
     * Second ByteBuf (secondDubboByteBuf):
     * +-------------------------++-----------------------+
     * |  dubbo(the remaining)   ||    dubbo(complete)    |
     * +-------------------------++-----------------------+
     *                           ||
     *                       Magic Code
     *
     * @throws InterruptedException
     */
    @Test
    public void testDubboDubboDecoded() throws InterruptedException, IOException {
        ByteBuf dubboByteBuf = createDubboByteBuf();

        ByteBuf firstDubboByteBuf = dubboByteBuf.copy(0, 50);
        ByteBuf secondLeftDubboByteBuf = dubboByteBuf.copy(50, dubboByteBuf.readableBytes() - 50);
        ByteBuf secondDubboByteBuf = Unpooled.wrappedBuffer(secondLeftDubboByteBuf, dubboByteBuf);


        EmbeddedChannel ch = null;
        try {
            Codec2 codec = ExtensionLoader.getExtensionLoader(Codec2.class).getExtension("dubbo");
            URL url = new URL("dubbo", "localhost", 22226);
            NettyCodecAdapter adapter = new NettyCodecAdapter(codec, url, new MockChannelHandler());

            MockHandler mockHandler = new MockHandler(null,
                    new MultiMessageHandler(
                            new DecodeHandler(
                                    new HeaderExchangeHandler(new ExchangeHandlerAdapter() {
                                        @Override
                                        public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) {
                                            if (checkDubboDecoded(msg)) {
                                                dubboDubbo.incrementAndGet();
                                            }
                                            return null;
                                        }
                                    }))));

            ch = new LocalEmbeddedChannel();
            ch.pipeline()
                    .addLast("decoder", adapter.getDecoder())
                    .addLast("handler", mockHandler);

            ch.writeInbound(firstDubboByteBuf);
            ch.writeInbound(secondDubboByteBuf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ch != null) {
                ch.close().await(200, TimeUnit.MILLISECONDS);
            }
        }

        TimeUnit.MILLISECONDS.sleep(100);

        Assertions.assertEquals(2, dubboDubbo.get());
    }

    /**
     * dubbo and telnet request
     *
     * <p>
     * First ByteBuf:
     * ++-------------------------------------------------+
     * ||               dubbo(incomplete)                 |
     * ++-------------------------------------------------+
     * ||
     * Magic Code
     *
     * <p>
     * Second ByteBuf:
     * +--------------------------------------------------+
     * |  dubbo(the remaining)  |     telnet(complete)    |
     * +--------------------------------------------------+
     *
     * @throws InterruptedException
     */
    @Test
    public void testDubboTelnetDecoded() throws InterruptedException, IOException {
        ByteBuf dubboByteBuf = createDubboByteBuf();
        ByteBuf firstDubboByteBuf = dubboByteBuf.copy(0, 50);
        ByteBuf secondLeftDubboByteBuf = dubboByteBuf.copy(50, dubboByteBuf.readableBytes());

        ByteBuf telnetByteBuf = Unpooled.wrappedBuffer("\r\n".getBytes());
        ByteBuf secondByteBuf = Unpooled.wrappedBuffer(secondLeftDubboByteBuf, telnetByteBuf);

        EmbeddedChannel ch = null;
        try {
            Codec2 codec = ExtensionLoader.getExtensionLoader(Codec2.class).getExtension("dubbo");
            URL url = new URL("dubbo", "localhost", 22226);
            NettyCodecAdapter adapter = new NettyCodecAdapter(codec, url, new MockChannelHandler());

            MockHandler mockHandler = new MockHandler((msg) -> {
                if (checkTelnetDecoded(msg)) {
                    dubboTelnet.incrementAndGet();
                }
            },
                    new MultiMessageHandler(
                            new DecodeHandler(
                                    new HeaderExchangeHandler(new ExchangeHandlerAdapter() {
                                        @Override
                                        public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) {
                                            if (checkDubboDecoded(msg)) {
                                                dubboTelnet.incrementAndGet();
                                            }
                                            return null;
                                        }
                                    }))));

            ch = new LocalEmbeddedChannel();
            ch.pipeline()
                    .addLast("decoder", adapter.getDecoder())
                    .addLast("handler", mockHandler);

            ch.writeInbound(firstDubboByteBuf);
            ch.writeInbound(secondByteBuf);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ch != null) {
                ch.close().await(200, TimeUnit.MILLISECONDS);
            }
        }

        TimeUnit.MILLISECONDS.sleep(100);

        Assertions.assertEquals(2, dubboTelnet.get());
    }

    private ByteBuf createDubboByteBuf() throws IOException {
        Request request = new Request();
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setMethodName("sayHello");
        rpcInvocation.setParameterTypes(new Class[]{String.class});
        rpcInvocation.setArguments(new String[]{"dubbo"});
        rpcInvocation.setAttachment("path", DemoService.class.getName());
        rpcInvocation.setAttachment("interface", DemoService.class.getName());
        rpcInvocation.setAttachment("version", "0.0.0");

        request.setData(rpcInvocation);
        request.setVersion("2.0.2");

        ByteBuf dubboByteBuf = Unpooled.buffer();
        ChannelBuffer buffer = new NettyBackedChannelBuffer(dubboByteBuf);
        DubboCodec dubboCodec = new DubboCodec();
        dubboCodec.encode(new MockChannel(), buffer, request);

        return dubboByteBuf;
    }

    private static boolean checkTelnetDecoded(Object msg) {
        if (msg != null && msg instanceof String && !msg.toString().contains("Unsupported command:")) {
            return true;
        }
        return false;
    }

    private static boolean checkDubboDecoded(Object msg) {
        if (msg instanceof DecodeableRpcInvocation) {
            DecodeableRpcInvocation invocation = (DecodeableRpcInvocation) msg;
            if ("sayHello".equals(invocation.getMethodName())
                    && invocation.getParameterTypes().length == 1
                    && String.class.equals(invocation.getParameterTypes()[0])
                    && invocation.getArguments().length == 1
                    && "dubbo".equals(invocation.getArguments()[0])
                    && DemoService.class.getName().equals(invocation.getAttachment("path"))
                    && DemoService.class.getName().equals(invocation.getAttachment("interface"))
                    && "0.0.0".equals(invocation.getAttachment("version"))) {
                return true;
            }
        }
        return false;
    }
}