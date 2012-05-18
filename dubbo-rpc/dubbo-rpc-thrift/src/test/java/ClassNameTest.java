/**
 * File Created at 2012-01-09
 * $Id$
 *
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */

import com.alibaba.dubbo.rpc.protocol.thrift.ThriftUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ClassNameTest {

    @Test
    public void testThriftUtils() {

        Assert.assertEquals( $__ClassNameTestDubboStub.echo_args.class.getName(),
                             ThriftUtils.generateMethodArgsClassName(
                                     ClassNameTestDubbo.class.getName(), "echo" ) );

        Assert.assertEquals( $__ClassNameTestDubboStub.echo_result.class.getName(),
                             ThriftUtils.generateMethodResultClassName(
                                     ClassNameTestDubbo.class.getName(), "echo" ) );

        Assert.assertEquals( ClassNameTestThrift.echo_args.class.getName(),
                             ThriftUtils.generateMethodArgsClassNameThrift(
                                     ClassNameTestThrift.Iface.class.getName(), "echo" ) );

        Assert.assertEquals( ClassNameTestThrift.echo_result.class.getName(),
                             ThriftUtils.generateMethodResultClassNameThrift(
                                     ClassNameTestThrift.Iface.class.getName(), "echo" ));

    }

}
