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
package org.apache.dubbo.common.extension.ext6_inject.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ext1.SimpleExt;
import org.apache.dubbo.common.extension.ext6_inject.Dao;
import org.apache.dubbo.common.extension.ext6_inject.Ext6;

import org.junit.jupiter.api.Assertions;

public class Ext6Impl1 implements Ext6 {
    public Dao obj;
    SimpleExt ext1;

    public void setDao(Dao obj) {
        Assertions.assertNotNull(obj, "inject extension instance can not be null");
        Assertions.fail();
    }

    public void setExt1(SimpleExt ext1) {
        this.ext1 = ext1;
    }

    public String echo(URL url, String s) {
        return "Ext6Impl1-echo-" + ext1.echo(url, s);
    }


}