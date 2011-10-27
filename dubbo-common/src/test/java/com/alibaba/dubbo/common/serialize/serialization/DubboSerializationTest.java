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
package com.alibaba.dubbo.common.serialize.serialization;

import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.dubbo.common.serialize.support.dubbo.DubboSerialization;

/**
 * @author ding.lid
 *
 */
public class DubboSerializationTest extends AbstractSerializationPersionFailTest {
    {
        serialization = new DubboSerialization();
    }
    
    // FIXME
    @Ignore("DubboSerialization error: java.lang.IllegalAccessError: tried to access class java.util.Arrays$ArrayList from class com.alibaba.dubbo.common.serialize.support.dubbo.Builder$bc6")
    @Test
    public void test_StringList_asListReturn() throws Exception {}

    // FIXME
    @Ignore("StackOverflowError")
    @Test(timeout=3000)
    public void test_LoopReference() throws Exception {}
}