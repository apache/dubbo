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
package com.alibaba.dubbo.common.utils;

import org.junit.Assert;
import org.junit.Test;

public class DubboxUtilTest {
    @Test
    public void dubboxTest() {
        String version1 = "2.5.4";
        org.junit.Assert.assertFalse(DubboxUtil.isDubbox(version1));

        String version2 = "2.8";
        org.junit.Assert.assertTrue(DubboxUtil.isDubbox(version2));


        String version3 = "2.8.1";
        org.junit.Assert.assertTrue(DubboxUtil.isDubbox(version3));

        String version4 = "2.8.2-SNAPSHOT";
        org.junit.Assert.assertTrue(DubboxUtil.isDubbox(version4));


        String version5 = "2.8.2.1";
        Assert.assertTrue(DubboxUtil.isDubbox(version5));


    }

}
