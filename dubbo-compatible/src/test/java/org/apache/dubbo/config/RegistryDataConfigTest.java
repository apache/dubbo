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
        registryDataConfig.setSimpleProviderUrl(false);
        registryDataConfig.setExtraProviderUrlParamKeys("xxx,sss");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testProviderNoParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleProviderUrl(true);
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get("simpleProviderUrl"), "true");
        Assert.assertNull(result.get("extraProviderUrlParamKeys"));
    }

    @Test
    public void testProviderHasParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleProviderUrl(true);
        registryDataConfig.setExtraProviderUrlParamKeys("xxx,sss");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get("simpleProviderUrl"), "true");
        Assert.assertEquals(result.get("extraProviderUrlParamKeys"), "xxx,sss");
    }

    @Test
    public void testConsumerNoValue(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleConsumerUrl(false);
        registryDataConfig.setExtraConsumerUrlParamKeys("xxx,sss");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testConsumerNoParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleConsumerUrl(true);
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get("simpleConsumerUrl"), "true");
        Assert.assertNull(result.get("extraConsumerUrlParamKeys"));
    }

    @Test
    public void testConsumerHasParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleConsumerUrl(true);
        registryDataConfig.setExtraConsumerUrlParamKeys("xxx,sss");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertFalse(result.isEmpty());
        Assert.assertEquals(result.get("simpleConsumerUrl"), "true");
        Assert.assertEquals(result.get("extraConsumerUrlParamKeys"), "xxx,sss");
    }

    @Test
    public void testMixHasParamKey(){
        RegistryDataConfig registryDataConfig = new RegistryDataConfig();
        registryDataConfig.setSimpleConsumerUrl(true);
        registryDataConfig.setExtraConsumerUrlParamKeys("xxx,sss");
        registryDataConfig.setSimpleProviderUrl(true);
        registryDataConfig.setExtraProviderUrlParamKeys("yyy,xxx");
        Map<String,String> result = registryDataConfig.transferToMap();
        Assert.assertTrue(result.size() == 4);
        Assert.assertEquals(result.get("simpleProviderUrl"), "true");
        Assert.assertEquals(result.get("extraProviderUrlParamKeys"), "yyy,xxx");
        Assert.assertEquals(result.get("simpleConsumerUrl"), "true");
        Assert.assertEquals(result.get("extraConsumerUrlParamKeys"), "xxx,sss");
    }
}
