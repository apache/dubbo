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

import org.apache.dubbo.rpc.protocol.thrift.ThriftUtils;

import org.junit.Assert;
import org.junit.Test;

public class ClassNameTest {

    @Test
    public void testThriftUtils() {

        Assert.assertEquals($__ClassNameTestDubboStub.echo_args.class.getName(),
                ThriftUtils.generateMethodArgsClassName(
                        ClassNameTestDubbo.class.getName(), "echo"));

        Assert.assertEquals($__ClassNameTestDubboStub.echo_result.class.getName(),
                ThriftUtils.generateMethodResultClassName(
                        ClassNameTestDubbo.class.getName(), "echo"));

        Assert.assertEquals(ClassNameTestThrift.echo_args.class.getName(),
                ThriftUtils.generateMethodArgsClassNameThrift(
                        ClassNameTestThrift.Iface.class.getName(), "echo"));

        Assert.assertEquals(ClassNameTestThrift.echo_result.class.getName(),
                ThriftUtils.generateMethodResultClassNameThrift(
                        ClassNameTestThrift.Iface.class.getName(), "echo"));

    }

}
