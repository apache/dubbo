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
package org.apache.dubbo.remoting.transport.netty4;

import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.SystemPropertyUtil;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ThreadFactory;

public class AbstractTransport {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTransport.class);

    protected URL url;
    protected ConfigOption<Integer> nThreads;
    protected ConfigOption<Boolean> epoll;

    protected TransporterConfig config;

    public AbstractTransport(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
        this.config = new TransporterConfig();

        this.epoll = ConfigOption.valueOf(Constants.EPOLL_ENABLE);
        this.nThreads = ConfigOption.valueOf(Constants.IO_THREADS_KEY);

        /**
         * Record default attribute values
         */
        this.config.option(epoll, url.getParameter(Constants.EPOLL_ENABLE, false) && epollAvailable())
                .option(nThreads, url.getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS));
    }

    public NioEventLoopGroup nioEventLoopGroup(ThreadFactory threadFactory) {
        return new NioEventLoopGroup(config.option(nThreads), threadFactory);
    }

    public NioEventLoopGroup nioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return new NioEventLoopGroup(nThreads, threadFactory);
    }

    public EpollEventLoopGroup epollEventLoopGroup(ThreadFactory threadFactory) {
        return new EpollEventLoopGroup(config.option(nThreads), threadFactory);
    }

    public EpollEventLoopGroup epollEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return new EpollEventLoopGroup(nThreads, threadFactory);
    }

    public URL getUrl() {
        return url;
    }

    protected boolean supportEpoll() {
        return config.option(epoll);
    }

    private boolean epollAvailable() {
        boolean linux = SystemPropertyUtil.get("os.name", "").toLowerCase(Locale.US).contains("linux");
        if (linux && logger.isDebugEnabled()) {
            logger.debug("Platform: Linux");
        }
        return linux && Epoll.isAvailable();
    }
}
