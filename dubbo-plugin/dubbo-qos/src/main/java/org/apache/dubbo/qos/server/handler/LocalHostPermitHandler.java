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
package org.apache.dubbo.qos.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.common.QosConstants;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class LocalHostPermitHandler extends ChannelHandlerAdapter {

    // true means to accept foreign IP
    private  boolean acceptForeignIp;

    // the whitelist of foreign IP when acceptForeignIp = false, separated by semicolon(;)
    // if the whitelist item start with `~`, treat it as escaped regex string
    private String acceptForeignIpWhitelist;
    private Predicate<String> whitelistPredicate = foreignIp -> false;

    public LocalHostPermitHandler(boolean acceptForeignIp, String foreignIpWhiteList) {
        this.acceptForeignIp = acceptForeignIp;
        this.acceptForeignIpWhitelist = foreignIpWhiteList;
        if (StringUtils.isNotEmpty(foreignIpWhiteList)) {
            whitelistPredicate = Arrays.stream(foreignIpWhiteList.split(";"))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .map(item -> (Predicate<String>) (foreignIp) -> {
                    if (!item.startsWith("~")) {
                        return StringUtils.isEquals(item, foreignIp);
                    }
                    // if the whitelist item start with `~`, treat it as regex
                    return Pattern.compile(item.substring(1)).matcher(foreignIp).matches();
                })
                .reduce(Predicate::or).orElse(s -> false);
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (acceptForeignIp) {
            return;
        }

        final InetAddress inetAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
        // loopback address, return
        if (inetAddress.isLoopbackAddress()) {
            return;
        }

        // the ip is in the whitelist, return
        if (checkForeignIpInWhiteList(inetAddress)) {
            return;
        }

        ByteBuf cb = Unpooled.wrappedBuffer((QosConstants.BR_STR + "Foreign Ip Not Permitted, Consider Config It In Whitelist."
            + QosConstants.BR_STR).getBytes());
        ctx.writeAndFlush(cb).addListener(ChannelFutureListener.CLOSE);
    }

    private boolean checkForeignIpInWhiteList(InetAddress inetAddress) {
        if (StringUtils.isEmpty(acceptForeignIpWhitelist)) {
            return false;
        }

        final String foreignIp = inetAddress.getHostAddress();
        return whitelistPredicate.test(foreignIp);
    }
}
