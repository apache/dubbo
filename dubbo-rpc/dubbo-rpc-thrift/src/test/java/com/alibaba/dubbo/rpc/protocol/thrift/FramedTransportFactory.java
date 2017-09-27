/**
 * File Created at 2011-12-09
 * $Id$
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.protocol.thrift;

import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportFactory;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class FramedTransportFactory extends TTransportFactory {

    @Override
    public TTransport getTransport(TTransport trans) {

        return new TFramedTransport(trans);
    }
}
