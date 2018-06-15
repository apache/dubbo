package org.apache.dubbo.compatible.cache;

import com.alibaba.dubbo.cache.Cache;
import com.alibaba.dubbo.cache.CacheFactory;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;

import org.apache.dubbo.rpc.RpcInvocation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class CacheTest {

    @Test
    public void testCacheFactory() {
        URL url = URL.valueOf("test://test:11/test?cache=jacache&.cache.write.expire=1");
        CacheFactory cacheFactory = new MyCacheFactory();
        Invocation invocation = new NullInvocation();
        Cache cache = cacheFactory.getCache(url, invocation);
        cache.put("testKey", "testValue");

        org.apache.dubbo.cache.CacheFactory factory = cacheFactory;
        org.apache.dubbo.common.URL u = org.apache.dubbo.common.URL.valueOf("test://test:11/test?cache=jacache&.cache.write.expire=1");
        org.apache.dubbo.rpc.Invocation inv = new RpcInvocation();
        org.apache.dubbo.cache.Cache c = factory.getCache(u, inv);
        String v = (String) c.get("testKey");
        Assert.assertEquals("testValue", v);
    }

    static class NullInvocation implements Invocation {
        @Override
        public String getMethodName() {
            return null;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return new Class[0];
        }

        @Override
        public Object[] getArguments() {
            return new Object[0];
        }

        @Override
        public Map<String, String> getAttachments() {
            return null;
        }

        @Override
        public String getAttachment(String key) {
            return null;
        }

        @Override
        public String getAttachment(String key, String defaultValue) {
            return null;
        }

        @Override
        public Invoker<?> getInvoker() {
            return null;
        }
    }
}
