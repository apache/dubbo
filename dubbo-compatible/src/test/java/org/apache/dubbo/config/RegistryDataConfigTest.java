package org.apache.dubbo.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * @author cvictory ON 2018/11/14
 */
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
