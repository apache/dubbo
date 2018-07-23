package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.support.DemoService;
import org.apache.dubbo.rpc.support.MyInvoker;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URLClassLoader;

public class ClassLoaderFilterTest {

    private ClassLoaderFilter classLoaderFilter = new ClassLoaderFilter();

    @Test
    public void testInvoke() throws Exception {
        URL url = URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1");

        String path = DemoService.class.getResource("/").getPath();
        final URLClassLoader cl = new URLClassLoader(new java.net.URL[]{new java.net.URL("file:" + path)}) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                try {
                    return findClass(name);
                } catch (ClassNotFoundException e) {
                    return super.loadClass(name);
                }
            }
        };
        final Class<?> clazz = cl.loadClass(DemoService.class.getCanonicalName());
        Invoker invoker = new MyInvoker(url) {
            @Override
            public Class getInterface() {
                return clazz;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                Assert.assertEquals(cl, Thread.currentThread().getContextClassLoader());
                return null;
            }
        };
        Invocation invocation = Mockito.mock(Invocation.class);

        classLoaderFilter.invoke(invoker, invocation);
    }
}
