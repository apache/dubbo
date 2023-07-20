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
package org.apache.dubbo.config.utils;

import org.junit.Assert;
import org.junit.Test;


public class SubnetUtilTest {
    @Test
    public void testLoadContent() {
        String content = "" +//
            "cn|cn-northwest|cell-1: \n" +
            "- 172.37.66.0/24 #cn-northwest-1a\n" +
            "cn|cn-north|cell-2: \n" +
            "- 172.37.67.0/24 #cn-northwest-1b\n" +
            "\"\": \n" +
            "- 172.37.33.0/24 #cn-north-1a\n";
        SubnetUtil.init(content);
        Assert.assertEquals(SubnetUtil.getTagLevelByHost("172.37.66.1"),"cn|cn-northwest|cell-1");
        Assert.assertEquals(SubnetUtil.getTagLevelByHost("172.37.33.1"),"");
    }
}
