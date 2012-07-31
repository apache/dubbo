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
package com.alibaba.dubbo.common.extensionloader.ext6_inject.impl;

import junit.framework.Assert;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt;
import com.alibaba.dubbo.common.extensionloader.ext6_inject.Dao;
import com.alibaba.dubbo.common.extensionloader.ext6_inject.Ext6;

/**
 * @author ding.lid
 */
public class Ext6Impl1 implements Ext6 {
    SimpleExt ext1;
    public Dao obj;
    
    public void setDao(Dao obj){
        Assert.assertNotNull("inject extension instance can not be null", obj);
        Assert.fail();
    }
    
    public void setExt1(SimpleExt ext1) {
        this.ext1 = ext1;
    }

    public String echo(URL url, String s) {
        return "Ext6Impl1-echo-" + ext1.echo(url, s);
    }
    

}