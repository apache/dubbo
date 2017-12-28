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
package com.alibaba.dubbo.governance.web.sysinfo.module.screen;

import com.alibaba.dubbo.common.logger.Level;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.User;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 */
public class Logs extends Restful {

    private static final int SHOW_LOG_LENGTH = 30000;

    public void index(Map<String, Object> context) throws Exception {
        long size;
        String content;
        String modified;
        File file = LoggerFactory.getFile();
        if (file != null && file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            FileChannel channel = fis.getChannel();
            size = channel.size();
            ByteBuffer bb;
            if (size <= SHOW_LOG_LENGTH) {
                bb = ByteBuffer.allocate((int) size);
                channel.read(bb, 0);
            } else {
                int pos = (int) (size - SHOW_LOG_LENGTH);
                bb = ByteBuffer.allocate(SHOW_LOG_LENGTH);
                channel.read(bb, pos);
            }
            bb.flip();
            content = new String(bb.array()).replace("<", "&lt;").replace(">", "&gt;");
            modified = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified()));
        } else {
            size = 0;
            content = "";
            modified = "Not exist";
        }
        Level level = LoggerFactory.getLevel();
        context.put("name", file == null ? "" : file.getAbsoluteFile());
        context.put("size", String.valueOf(size));
        context.put("level", level == null ? "" : level);
        context.put("modified", modified);
        context.put("content", content);
    }

    public boolean change(Map<String, Object> context) throws Exception {
        String contextLevel = (String) context.get("level");
        if (contextLevel == null || contextLevel.length() == 0) {
            context.put("message", getMessage("MissRequestParameters", "level"));
            return false;
        }
        if (!User.ROOT.equals(role)) {
            context.put("message", getMessage("HaveNoRootPrivilege"));
            return false;
        }
        Level level = Level.valueOf(contextLevel);
        if (level != LoggerFactory.getLevel()) {
            LoggerFactory.setLevel(level);
        }
        context.put("redirect", "/sysinfo/logs");
        return true;
    }
}
