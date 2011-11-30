/**
 * Project: dubbo-remoting
 * 
 * File Created at 2011-11-30
 * $Id$
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.remoting.ChannelHandler;

/**
 * @author chao.liuc
 */
public interface ChannelHandlerDelegate extends ChannelHandler {
    public ChannelHandler getHandler();
}
