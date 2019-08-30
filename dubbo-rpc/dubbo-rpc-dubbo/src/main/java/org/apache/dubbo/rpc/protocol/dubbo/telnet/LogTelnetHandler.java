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
package org.apache.dubbo.rpc.protocol.dubbo.telnet;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Level;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.TelnetHandler;
import org.apache.dubbo.remoting.telnet.support.Help;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * LogTelnetHandler
 */
@Activate
@Help(parameter = "level", summary = "Change log level or show log ", detail = "Change log level or show log")
public class LogTelnetHandler implements TelnetHandler {

    public static final String SERVICE_KEY = "telnet.log";

    @Override
    public String telnet(Channel channel, String message) {
        long size;
        File file = LoggerFactory.getFile();
        StringBuffer buf = new StringBuffer();
        if (message == null || message.trim().length() == 0) {
            buf.append("EXAMPLE: log error / log 100");
        } else {
            String[] str = message.split(" ");
            if (!StringUtils.isInteger(str[0])) {
                LoggerFactory.setLevel(Level.valueOf(message.toUpperCase()));
            } else {
                int showLogLength = Integer.parseInt(str[0]);

                if (file != null && file.exists()) {
                    try(FileInputStream fis = new FileInputStream(file)) {
                        FileChannel filechannel = fis.getChannel();
                        size = filechannel.size();
                        ByteBuffer bb;
                        if (size <= showLogLength) {
                            bb = ByteBuffer.allocate((int) size);
                            filechannel.read(bb, 0);
                        } else {
                            int pos = (int) (size - showLogLength);
                            bb = ByteBuffer.allocate(showLogLength);
                            filechannel.read(bb, pos);
                        }
                        bb.flip();
                        String content = new String(bb.array()).replace("<", "&lt;")
                                .replace(">", "&gt;").replace("\n", "<br/><br/>");
                        buf.append("\r\ncontent:").append(content);

                        buf.append("\r\nmodified:").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                .format(new Date(file.lastModified())));
                        buf.append("\r\nsize:").append(size).append("\r\n");
                    } catch (Exception e) {
                        buf.append(e.getMessage());
                    }
                } else {
                    buf.append("\r\nMESSAGE: log file not exists or log appender is console .");
                }
            }
        }
        buf.append("\r\nCURRENT LOG LEVEL:").append(LoggerFactory.getLevel()).append("\r\nCURRENT LOG APPENDER:")
                .append(file == null ? "console" : file.getAbsolutePath());
        return buf.toString();
    }

}
