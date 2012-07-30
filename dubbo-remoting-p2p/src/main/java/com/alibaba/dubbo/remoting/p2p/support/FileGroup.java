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
package com.alibaba.dubbo.remoting.p2p.support;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.IOUtils;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.p2p.Peer;

/**
 * FileGroup
 * 
 * @author william.liangf
 */
public class FileGroup extends AbstractGroup {
    
    private final File file;
    
    private volatile long last;

    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3, new NamedThreadFactory("FileGroupModifiedChecker", true));

    // 重连定时器，定时检查连接是否可用，不可用时，无限次重连
    private final ScheduledFuture<?> checkModifiedFuture;

    public FileGroup(URL url){
        super(url);
        String path = url.getAbsolutePath();
        file = new File(path);
        checkModifiedFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 检测文件变更
                try {
                    check();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at reconnect, cause: " + t.getMessage(), t);
                }
            }
        }, 2000, 2000, TimeUnit.MILLISECONDS);
    }

    public void close() {
        super.close();
        try {
            if (! checkModifiedFuture.isCancelled()) {
                checkModifiedFuture.cancel(true);
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    private void check() throws RemotingException {
        long modified = file.lastModified();
        if (modified > last) {
            last = modified;
            changed();
        }
    }
    
    private void changed() throws RemotingException {
        try {
            String[] lines = IOUtils.readLines(file);
            for (String line : lines) {
                connect(URL.valueOf(line));
            }
        } catch (IOException e) {
            throw new RemotingException(new InetSocketAddress(NetUtils.getLocalHost(), 0), getUrl().toInetSocketAddress(), e.getMessage(), e);
        }
    }

    public Peer join(URL url, ChannelHandler handler) throws RemotingException {
        Peer peer = super.join(url, handler);
        try {
            String full = url.toFullString();
            String[] lines = IOUtils.readLines(file);
            for (String line : lines) {
                if (full.equals(line)) {
                    return peer;
                }
            }
            IOUtils.appendLines(file, new String[] {full});
        } catch (IOException e) {
            throw new RemotingException(new InetSocketAddress(NetUtils.getLocalHost(), 0), getUrl().toInetSocketAddress(), e.getMessage(), e);
        }
        return peer;
    }
    
    @Override
    public void leave(URL url) throws RemotingException {
        super.leave(url);
        try {
            String full = url.toFullString();
            String[] lines = IOUtils.readLines(file);
            List<String> saves = new ArrayList<String>();
            for (String line : lines) {
                if (full.equals(line)) {
                    return;
                }
                saves.add(line);
            }
            IOUtils.appendLines(file, saves.toArray(new String[0]));
        } catch (IOException e) {
            throw new RemotingException(new InetSocketAddress(NetUtils.getLocalHost(), 0), getUrl().toInetSocketAddress(), e.getMessage(), e);
        }
    }

}