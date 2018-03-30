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
package com.alibaba.dubbo.remoting.telnet.codec;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.buffer.ChannelBuffer;
import com.alibaba.dubbo.remoting.transport.codec.TransportCodec;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * TelnetCodec
 *
 * Telnet 命令编解码器
 */
public class TelnetCodec extends TransportCodec {

    private static final Logger logger = LoggerFactory.getLogger(TelnetCodec.class);

    /**
     * 历史命令列表
     */
    private static final String HISTORY_LIST_KEY = "telnet.history.list";
    /**
     * 历史命令位置（用户向上或向下）
     */
    private static final String HISTORY_INDEX_KEY = "telnet.history.index";
    /**
     * 向上
     */
    private static final byte[] UP = new byte[]{27, 91, 65};
    /**
     * 向下
     */
    private static final byte[] DOWN = new byte[]{27, 91, 66};

    /**
     * 回车
     */
    private static final List<?> ENTER = Arrays.asList(new Object[]{new byte[]{'\r', '\n'} /* Windows Enter */, new byte[]{'\n'} /* Linux Enter */});

    /**
     * 退出
     */
    private static final List<?> EXIT = Arrays.asList(new Object[]{new byte[]{3} /* Windows Ctrl+C */, new byte[]{-1, -12, -1, -3, 6} /* Linux Ctrl+C */, new byte[]{-1, -19, -1, -3, 6} /* Linux Pause */});

    /**
     * 获得通道的字符集
     *
     * @param channel 通道
     * @return 字符集
     */
    private static Charset getCharset(Channel channel) {
        if (channel != null) {
            // 从 通道属性 获得字符集
            Object attribute = channel.getAttribute(Constants.CHARSET_KEY);
            if (attribute instanceof String) {
                try {
                    return Charset.forName((String) attribute);
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            } else if (attribute instanceof Charset) {
                return (Charset) attribute;
            }
            // 从 URL属性 获得字符集
            URL url = channel.getUrl();
            if (url != null) {
                String parameter = url.getParameter(Constants.CHARSET_KEY);
                if (parameter != null && parameter.length() > 0) {
                    try {
                        return Charset.forName(parameter);
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                    }
                }
            }
        }
        // 使用默认字符集
        try {
            return Charset.forName(Constants.DEFAULT_CHARSET);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        return Charset.defaultCharset();
    }

    /**
     * 将命令字节数组，转成具体的一条命令。
     *
     * @param message 命令字节数组
     * @param charset  字符集
     * @return telnet 命令
     * @throws UnsupportedEncodingException 当字符集不支持
     */
    private static String toString(byte[] message, Charset charset) throws UnsupportedEncodingException {
        byte[] copy = new byte[message.length];
        int index = 0;
        for (int i = 0; i < message.length; i++) {
            byte b = message[i];
            // 退格，尾部减小
            if (b == '\b') { // backspace
                if (index > 0) {
                    index--;
                }
                if (i > 2 && message[i - 2] < 0) { // double byte char
                    if (index > 0) {
                        index--;
                    }
                }
            // 换码(溢出)
            } else if (b == 27) { // escape
                if (i < message.length - 4 && message[i + 4] == 126) {
                    i = i + 4;
                } else if (i < message.length - 3 && message[i + 3] == 126) {
                    i = i + 3;
                } else if (i < message.length - 2) {
                    i = i + 2;
                }
            // 握手
            } else if (b == -1 && i < message.length - 2
                    && (message[i + 1] == -3 || message[i + 1] == -5)) { // handshake
                i = i + 2;
            } else {
                copy[index++] = message[i];
            }
        }
        if (index == 0) {
            return "";
        }
        // 创建字符串
        return new String(copy, 0, index, charset.name()).trim();
    }

    private static boolean isEquals(byte[] message, byte[] command) throws IOException {
        return message.length == command.length && endsWith(message, command);
    }

    private static boolean endsWith(byte[] message, byte[] command) throws IOException {
        if (message.length < command.length) {
            return false;
        }
        int offset = message.length - command.length;
        for (int i = command.length - 1; i >= 0; i--) {
            if (message[offset + i] != command[i]) {
                return false;
            }
        }
        return true;
    }

    public void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException {
        // telnet 命令结果
        if (message instanceof String) {
            if (isClientSide(channel)) { // 【TODO 8025】为啥 client 侧，需要多加 \r\n
                message = message + "\r\n";
            }
            // 写入
            byte[] msgData = ((String) message).getBytes(getCharset(channel).name());
            buffer.writeBytes(msgData);
        // 非 telnet 命令结果。目前不会出现
        } else {
            super.encode(channel, buffer, message);
        }
    }

    @Override
    public Object decode(Channel channel, ChannelBuffer buffer) throws IOException {
        int readable = buffer.readableBytes();
        byte[] message = new byte[readable];
        buffer.readBytes(message);
        return decode(channel, buffer, readable, message);
    }

    @SuppressWarnings("unchecked")
    protected Object decode(Channel channel, ChannelBuffer buffer, int readable, byte[] message) throws IOException {
        // 【TODO 8025】为啥 client 侧，直接返回
        if (isClientSide(channel)) {
            return toString(message, getCharset(channel));
        }
        // 检查长度
        checkPayload(channel, readable);
        if (message == null || message.length == 0) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        // 处理退格的情况。
        if (message[message.length - 1] == '\b') { // Windows backspace echo
            try {
                // 32=空格 8=退格
                boolean doublechar = message.length >= 3 && message[message.length - 3] < 0; // double byte char
                channel.send(new String(doublechar ? new byte[]{32, 32, 8, 8} : new byte[]{32, 8}, getCharset(channel).name()));
            } catch (RemotingException e) {
                throw new IOException(StringUtils.toString(e));
            }
            return DecodeResult.NEED_MORE_INPUT;
        }

        // 关闭指令
        for (Object command : EXIT) {
            if (isEquals(message, (byte[]) command)) {
                if (logger.isInfoEnabled()) {
                    logger.info(new Exception("Close channel " + channel + " on exit command: " + Arrays.toString((byte[]) command)));
                }
                channel.close(); // 关闭通道
                return null;
            }
        }

        // 使用历史的命令
        boolean up = endsWith(message, UP);
        boolean down = endsWith(message, DOWN);
        if (up || down) {
            LinkedList<String> history = (LinkedList<String>) channel.getAttribute(HISTORY_LIST_KEY);
            if (history == null || history.isEmpty()) {
                return DecodeResult.NEED_MORE_INPUT;
            }
            // 获得历史命令数组的位置
            Integer index = (Integer) channel.getAttribute(HISTORY_INDEX_KEY);
            Integer old = index;
            if (index == null) {
                index = history.size() - 1;
            } else {
                if (up) { // 向上
                    index = index - 1;
                    if (index < 0) {
                        index = history.size() - 1;
                    }
                } else { // 向下
                    index = index + 1;
                    if (index > history.size() - 1) {
                        index = 0;
                    }
                }
            }
            // 获得历史命令，并发送给客户端
            if (old == null || !old.equals(index)) {
                // 设置当前位置
                channel.setAttribute(HISTORY_INDEX_KEY, index);
                // 获得历史命令
                String value = history.get(index);
                // 拼接退格，以清除客户端原有命令
                if (old != null && old >= 0 && old < history.size()) {
                    String ov = history.get(old);
                    StringBuilder buf = new StringBuilder();
                    for (int i = 0; i < ov.length(); i++) {
                        buf.append("\b"); // 退格
                    }
                    for (int i = 0; i < ov.length(); i++) {
                        buf.append(" ");
                    }
                    for (int i = 0; i < ov.length(); i++) {
                        buf.append("\b"); // 退格
                    }
                    value = buf.toString() + value;
                }
                // 发送命令
                try {
                    channel.send(value);
                } catch (RemotingException e) {
                    throw new IOException(StringUtils.toString(e));
                }
            }
            // 返回，需要更多指令
            return DecodeResult.NEED_MORE_INPUT;
        }

        // 关闭指令
        for (Object command : EXIT) {
            if (isEquals(message, (byte[]) command)) {
                if (logger.isInfoEnabled()) {
                    logger.info(new Exception("Close channel " + channel + " on exit command " + command));
                }
                channel.close();
                return null;
            }
        }
        // 查找是否回车结尾。若不是，说明一条 telnet 指令没结束。
        byte[] enter = null;
        for (Object command : ENTER) {
            if (endsWith(message, (byte[]) command)) {
                enter = (byte[]) command;
                break;
            }
        }
        if (enter == null) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        // 移除历史命令数组的位置
        LinkedList<String> history = (LinkedList<String>) channel.getAttribute(HISTORY_LIST_KEY);
        Integer index = (Integer) channel.getAttribute(HISTORY_INDEX_KEY);
        channel.removeAttribute(HISTORY_INDEX_KEY);
        // 将历史命令拼接
        if (history != null && !history.isEmpty() && index != null && index >= 0 && index < history.size()) {
            String value = history.get(index);
            if (value != null) {
                byte[] b1 = value.getBytes();
                byte[] b2 = new byte[b1.length + message.length];
                System.arraycopy(b1, 0, b2, 0, b1.length);
                System.arraycopy(message, 0, b2, b1.length, message.length);
                message = b2;
            }
        }
        // 将命令字节数组，转成具体的一条命令
        String result = toString(message, getCharset(channel));
        // 添加到历史
        if (result.trim().length() > 0) {
            if (history == null) {
                history = new LinkedList<String>();
                channel.setAttribute(HISTORY_LIST_KEY, history);
            }
            if (history.isEmpty()) {
                history.addLast(result);
            } else if (!result.equals(history.getLast())) {
                // 添加当前命令到历史尾部
                history.remove(result);
                history.addLast(result);
                // 超过上限，移除历史的头部
                if (history.size() > 10) {
                    history.removeFirst();
                }
            }
        }
        return result;
    }

}