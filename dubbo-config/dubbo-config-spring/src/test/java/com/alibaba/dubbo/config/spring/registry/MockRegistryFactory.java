package com.alibaba.dubbo.config.spring.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

/**
 * @author <a href="mailto:gang.lvg@taobao.com">kimi</a>
 */
public class MockRegistryFactory implements RegistryFactory {

    private static final Map<URL, Registry> registries = new HashMap<URL, Registry>();

    public Registry getRegistry(URL url) {
        MockRegistry registry = new MockRegistry(url);
        registries.put(url, registry);
        return registry;
    }

    public static Collection<Registry> getCachedRegistry() {
        return registries.values();
    }

    public static void cleanCachedRegistry() {
        registries.clear();
    }
}
