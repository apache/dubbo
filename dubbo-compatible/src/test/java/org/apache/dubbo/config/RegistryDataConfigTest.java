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
package org.apache.dubbo.config;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

@Ignore("Waiting for cvictory to fix")
public class RegistryDataConfigTest {

    @Test
    public void testProviderNoValue(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleProviderConfig(false);
        registryDataConfig.setExtraProviderKeys("xxx,sss");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testProviderNoParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleProviderConfig(true);
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get("simpleProviderConfig"), "true");
        Assert.assertNull(result.get("extraProviderKeys"));
    }

    @Test
    public void testProviderHasParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleProviderConfig(true);
        registryDataConfig.setExtraProviderKeys("xxx,sss");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get("simpleProviderConfig"), "true");
        Assert.assertEquals(result.get("extraProviderKeys"), "xxx,sss");
    }

    @Test
    public void testConsumerNoValue(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleConsumerConfig(false);
        registryDataConfig.setExtraConsumerKeys("xxx,sss");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testConsumerNoParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleConsumerConfig(true);
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get("simpleConsumerConfig"), "true");
        Assert.assertNull(result.get("extraConsumerKeys"));
    }

    @Test
    public void testConsumerHasParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleConsumerConfig(true);
        registryDataConfig.setExtraConsumerKeys("xxx,sss");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get("simpleConsumerConfig"), "true");
        Assert.assertEquals(result.get("extraConsumerKeys"), "xxx,sss");
    }

    @Test
    public void testMixHasParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleConsumerConfig(true);
        registryDataConfig.setExtraConsumerKeys("xxx,sss");
        registryDataConfig.setSimpleProviderConfig(true);
        registryDataConfig.setExtraProviderKeys("yyy,xxx");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertTrue(result.size() == 4);
        Assert.assertEquals(result.get("simpleProviderConfig"), "true");
        Assert.assertEquals(result.get("extraProviderKeys"), "yyy,xxx");
        Assert.assertEquals(result.get("simpleConsumerConfig"), "true");
        Assert.assertEquals(result.get("extraConsumerKeys"), "xxx,sss");
    }
}
