/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting.transport.grizzly;

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.io.Bytes;
import com.alibaba.dubbo.common.io.UnsafeByteArrayInputStream;
import com.alibaba.dubbo.common.io.UnsafeByteArrayOutputStream;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Codec;

/**
 * GrizzlyCodecAdapter
 * 
 * @author william.liangf
 */
public class GrizzlyCodecAdapter extends BaseFilter {

    private static final String   BUFFER_KEY = GrizzlyCodecAdapter.class.getName() + ".BUFFER";

    private final Codec           codec;

    private final URL             url;
    
    private final ChannelHandler  handler;

    private final int             bufferSize;
    
    public GrizzlyCodecAdapter(Codec codec, URL url, ChannelHandler handler) {
        this.codec = codec;
        this.url = url;
        this.handler = handler;
        int b = url.getPositiveParameter(Constants.BUFFER_KEY, Constants.DEFAULT_BUFFER_SIZE);
        this.bufferSize = b >= Constants.MIN_BUFFER_SIZE && b <= Constants.MAX_BUFFER_SIZE ? b : Constants.DEFAULT_BUFFER_SIZE;
    }

    @Override
    public NextAction handleWrite(FilterChainContext context) throws IOException {
        Connection<?> connection = context.getConnection();
        GrizzlyChannel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            UnsafeByteArrayOutputStream output = new UnsafeByteArrayOutputStream(1024); // 不需要关闭
            
            Object msg = context.getMessage();
            codec.encode(channel, output, msg);
            
            GrizzlyChannel.removeChannelIfDisconnectd(connection);
            byte[] bytes = output.toByteArray();
            Buffer buffer = connection.getTransport().getMemoryManager().allocate(bytes.length);
            buffer.put(bytes);
            buffer.flip();
            buffer.allowBufferDispose(true);
            context.setMessage(buffer);
        } finally {
            GrizzlyChannel.removeChannelIfDisconnectd(connection);
        }
        return context.getInvokeAction();
    }

    @Override
    public NextAction handleRead(FilterChainContext context) throws IOException {
        Object message = context.getMessage();
        Connection<?> connection = context.getConnection();
        Channel channel = GrizzlyChannel.getOrAddChannel(connection, url, handler);
        try {
            if (message instanceof Buffer) { // 收到新的数据包
                Buffer buffer = (Buffer) message; // 缓存
                int readable = buffer.capacity(); // 本次可读取新数据的大小
                if (readable == 0) {
                    return context.getStopAction();
                }
                byte[] bytes; // byte[]缓存区，将Buffer转成byte[]，再转成UnsafeByteArrayInputStream
                int offset; // 指向已用数据的偏移量，off之前的数据都是已用过的
                int limit; // 有效长度，limit之后的长度是空白或无效数据，off到limit之间的数据是准备使用的有效数据
                Object[] remainder = (Object[]) channel.getAttribute(BUFFER_KEY); // 上次序列化剩下的数据
                channel.removeAttribute(BUFFER_KEY);
                if (remainder == null) { // 如果没有，创建新的bytes缓存
                    bytes = new byte[bufferSize];
                    offset = 0;
                    limit = 0;
                } else { // 如果有，使用剩下的bytes缓存
                    bytes = (byte[]) remainder[0];
                    offset = (Integer) remainder[1];
                    limit = (Integer) remainder[2];
                }
                return receive(context, channel, buffer, readable, bytes, offset, limit);
            } else if (message instanceof Object[]) { // 同一Buffer多轮Filter，即：一个Buffer里有多个请求
                Object[] remainder = (Object[]) message;
                Buffer buffer = (Buffer) remainder[0];
                int readable = (Integer) remainder[1];
                byte[] bytes = (byte[]) remainder[2];
                int offset = (Integer) remainder[3];
                int limit = (Integer) remainder[4];
                return receive(context, channel, buffer, readable, bytes, offset, limit);
            } else { // 其它事件直接往下传
                return context.getInvokeAction();
            }
        } finally {
            GrizzlyChannel.removeChannelIfDisconnectd(connection);
        }
    }
    
    /*
     * 接收
     * 
     * @param context 上下文
     * @param channel 通道
     * @param buffer 缓存
     * @param readable 缓存可读
     * @param bytes 输入缓存
     * @param offset 指向已读数据的偏移量，off之前的数据都是已用过的
     * @param limit 有效长度，limit之后的长度是空白或无效数据，off到limit之间的数据是准备使用的数据
     * @return 后续动作
     * @throws IOException
     */
    private NextAction receive(FilterChainContext context, Channel channel, Buffer buffer, int readable, byte[] bytes, int offset, int limit) throws IOException {
        for(;;) {
            int read = Math.min(readable, bytes.length - limit); // 取bytes缓存空闲区，和可读取新数据，的最小值，即：此次最多读写数据的大小
            buffer.get(bytes, limit, read); // 从可读取新数据中，读取数据，尽量填满bytes缓存空闲区
            limit += read; // 有效数据变长
            readable -= read; // 可读数据变少
            UnsafeByteArrayInputStream input = new UnsafeByteArrayInputStream(bytes, offset, limit - offset); // 将bytes缓存转成InputStream，不需要关闭
            Object msg = codec.decode(channel, input); // 调用Codec接口，解码数据
            if (msg == Codec.NEED_MORE_INPUT) { // 如果Codec觉得数据不够，不足以解码成一个对象
                if (readable == 0) { // 如果没有更多可读数据
                    channel.setAttribute(BUFFER_KEY, new Object[] { bytes, offset, limit }); // 放入通道属性中，等待下一个Buffer的到来
                    return context.getStopAction();
                } else { // 扩充或挪出空闲区，并循环，直到可读数据都加载到bytes缓存
                    if (offset == 0) { // 如果bytes缓存全部没有被使用，如果这时数据还不够
                        bytes = Bytes.copyOf(bytes, bytes.length << 1); // 将bytes缓存扩大一倍
                    } else { // 如果bytes缓存有一段数据已被使用
                        int len = limit - offset; // 计算有效数据长度
                        System.arraycopy(bytes, offset, bytes, 0, len); // 将数据向前移到，压缩到已使用的部分，这样limit后面就会多出一些空闲，可以放数据
                        offset = 0; // 移到后，bytes缓存没有数据被使用
                        limit = len; // 移到后，有效数据都在bytes缓存最前面
                    }
                }
            } else { // 如果解析出一个结果
                int position = input.position(); // 记录InputStream用了多少
                if (position == offset) { // 如果InputStream没有被读过，就返回了数据，直接报错，否则InputStream永远读不完，会死递归
                    throw new IOException("Decode without read data.");
                }
                offset = position; // 记录已读数据
                context.setMessage(msg); // 将消息改为解码后的对象，以便被后面的Filter使用。
                if (limit - offset > 0 || readable > 0) { // 如果有效数据没有被读完，或者Buffer区还有未读数据
                    return context.getInvokeAction(new Object[] { buffer, readable, bytes, offset, limit }); // 正常执行完Filter，并重新发起一轮Filter，继续读
                } else { // 否则所有数据读完
                    return context.getInvokeAction(); // 正常执行完Filter
                }
            }
        }
    }

}