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
package com.alibaba.com.caucho.hessian.io;

import com.alibaba.com.caucho.hessian.io.base.SerializeTestBase;
import com.alibaba.com.caucho.hessian.io.beans.BaseUser;
import com.alibaba.com.caucho.hessian.io.beans.GrandsonUser;
import com.alibaba.com.caucho.hessian.io.beans.SubUser;

import org.junit.Assert;
import org.junit.Test;

/**
 * fix hessian serialize bug:
 * the filed of parent class will cover the filed of sub class
 *
 */
public class HessianJavaSerializeTest extends SerializeTestBase {

    @Test
    public void testGetBaseUserName() throws Exception {

        BaseUser baseUser = new BaseUser();
        baseUser.setUserId(1);
        baseUser.setUserName("tom");

        BaseUser serializedUser = baseHessianSerialize(baseUser);
        Assert.assertEquals("tom", serializedUser.getUserName());
    }


    @Test
    public void testGetSubUserName() throws Exception {
        SubUser subUser = new SubUser();
        subUser.setUserId(1);
        subUser.setUserName("tom");

        SubUser serializedUser = baseHessianSerialize(subUser);
        Assert.assertEquals("tom", serializedUser.getUserName());

    }

    @Test
    public void testGetGrandsonUserName() throws Exception {
        GrandsonUser grandsonUser = new GrandsonUser();
        grandsonUser.setUserId(1);
        grandsonUser.setUserName("tom");

        GrandsonUser serializedUser = baseHessianSerialize(grandsonUser);
        Assert.assertEquals("tom", serializedUser.getUserName());
    }

}
