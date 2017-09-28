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
package com.alibaba.dubbo.common.extensionloader.ext2.impl;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extensionloader.ext2.Ext2;
import com.alibaba.dubbo.common.extensionloader.ext2.UrlHolder;

/**
 * @author ding.lid
 */
public class Ext2Impl3 implements Ext2 {
    public String echo(UrlHolder holder, String s) {
        return "Ext2Impl3-echo";
    }

    public String bang(URL url, int i) {
        return "bang3";
    }

}