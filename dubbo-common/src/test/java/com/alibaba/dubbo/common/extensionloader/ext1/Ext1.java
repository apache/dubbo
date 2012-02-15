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
package com.alibaba.dubbo.common.extensionloader.ext1;

import com.alibaba.dubbo.common.Adaptive;
import com.alibaba.dubbo.common.Default;
import com.alibaba.dubbo.common.URL;

/**
 * @author ding.lid
 */
@Default("impl1")
public interface Ext1 {
    @Adaptive
    String echo(URL url, String s);
    
    @Adaptive({"key1", "key2"})
    String yell(URL url, String s);
    
    String bang(URL url, int i);
}