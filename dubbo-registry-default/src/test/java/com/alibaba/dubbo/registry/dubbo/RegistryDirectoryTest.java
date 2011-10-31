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
package com.alibaba.dubbo.registry.dubbo;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngineManager;

import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.RegistryFactory;
import com.alibaba.dubbo.registry.support.RegistryDirectory;
import com.alibaba.dubbo.registry.support.SimpleRegistryExporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance;
import com.alibaba.dubbo.rpc.cluster.router.ScriptRouter;
import com.alibaba.dubbo.rpc.cluster.router.ScriptRouterFactory;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class RegistryDirectoryTest {
    private static String service = "com.alibaba.dubbo.demo.DemoService";
    static {
        SimpleRegistryExporter.exportIfAbsent(9090);
        SimpleRegistryExporter.exportIfAbsent(9091);
        SimpleRegistryExporter.exportIfAbsent(9092);
        SimpleRegistryExporter.exportIfAbsent(9093);
    }
    public static URL  REGURL= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9090/"+service + "?callbacks=100");
    public static URL  SERVICEURL= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9091/"+service + "?callbacks=100");
    public static URL  SERVICEURL2= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9092/"+service + "?callbacks=100");
    public static URL  SERVICEURL3= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9093/"+service + "?callbacks=100");
    public static URL  SERVICEURL_DUBBO_NOPATH= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9092" + "?callbacks=100");
    
    public static URL  ROUTERURL= URL.valueOf(RpcConstants.ROUTE_PROTOCOL + "://"+NetUtils.getLocalHost()+":9096/");
    public static URL  ROUTERURL2= URL.valueOf(RpcConstants.ROUTE_PROTOCOL + "://"+NetUtils.getLocalHost()+":9097/");

    
    List invokers = null;
    RpcInvocation invocation = new RpcInvocation();
    RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
    Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    URL url = REGURL.addParameter("test", "reg");
    RegistryDirectory<RegistryDirectoryTest> registryDirectory = new RegistryDirectory(RegistryDirectoryTest.class, url);
    {
    registryDirectory.setRegistry(registryFactory.getRegistry(url));
    registryDirectory.setProtocol(protocol);
    }
    URL url2 = REGURL.addParameter("test", "reg").addParameterAndEncoded(RpcConstants.REFER_KEY, 
            "key=query&" + Constants.LOADBALANCE_KEY + "=" + LeastActiveLoadBalance.NAME);
    RegistryDirectory<RegistryDirectoryTest> registryDirectory2 = new RegistryDirectory(RegistryDirectoryTest.class, url2);
    {
    registryDirectory2.setRegistry(registryFactory.getRegistry(url2));
    registryDirectory2.setProtocol(protocol);
    }
    
    @Test
    public void testNotified() {
        
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(0,invokers.size());
        
        test2invokers();
        test1invokers();
        test3invokers();
        testforbid();
        
    }
    //forbid
    private void testforbid(){
        invocation = new RpcInvocation();
        List<URL> serviceUrls = new ArrayList<URL> ();
        registryDirectory.notify(serviceUrls);
        try {
            invokers = registryDirectory.list(invocation);
            fail("forbid must throw RpcException");
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.FORBIDDEN_EXCEPTION, e.getCode());
        }
      }
    
    //通知成一个invoker===================================
    private void test1invokers(){
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));//.addParameter("refer.autodestroy", "true")
        registryDirectory.notify(serviceUrls);

        invocation = new RpcInvocation();
        
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1,invokers.size());
        
        invocation.setMethodName("getXXX");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1,invokers.size());
        
        invocation.setMethodName("getXXX1");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1,invokers.size());
        
        invocation.setMethodName("getXXX2");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1,invokers.size());
    }
    
    //两个invoker===================================
    private void test2invokers(){
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));

        registryDirectory.notify(serviceUrls);
      
        
        invocation = new RpcInvocation();
        
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(2,invokers.size());
        
        invocation.setMethodName("getXXX");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(2,invokers.size());
        
        invocation.setMethodName("getXXX1");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(2,invokers.size());
        
        invocation.setMethodName("getXXX2");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1,invokers.size());
    }
    
    //通知成3个invoker===================================
    private void test3invokers(){
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1,getXXX2,getXXX3"));

        registryDirectory.notify(serviceUrls);
        
        invocation = new RpcInvocation();
           
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(3,invokers.size());
        

        invocation.setMethodName("getXXX");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(3,invokers.size());
        
        invocation.setMethodName("getXXX1");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(3,invokers.size());
        
        invocation.setMethodName("getXXX2");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(2,invokers.size());
        
        invocation.setMethodName("getXXX3");
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1,invokers.size());
    }
    
    @Test
    public  void testParametersMerge(){
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        //检验注册中心的参数需要被清除
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
            registryDirectory.notify(serviceUrls);
            
            invocation = new RpcInvocation();        
            invokers = registryDirectory.list(invocation);
            
            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assert.assertEquals(null, url.getParameter("key"));
        }
        //检验服务提供方的参数需要merge
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX2").addParameter("key", "provider"));
            
            registryDirectory.notify(serviceUrls);
            invocation = new RpcInvocation();
            invokers = registryDirectory.list(invocation);
            
            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assert.assertEquals("provider",url.getParameter("key"));
        }
        //检验服务query的参数需要与providermerge 。
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX3").addParameter("key", "provider"));
            
            registryDirectory2.notify(serviceUrls);
            invocation = new RpcInvocation();
            invokers = registryDirectory2.list(invocation);
            
            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            System.out.println(url);
            Assert.assertEquals("query",url.getParameter("key"));
        }
        
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
            registryDirectory.notify(serviceUrls);
            
            invocation = new RpcInvocation();        
            invokers = registryDirectory.list(invocation);
            
            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assert.assertEquals(false,url.getParameter(Constants.CHECK_KEY, false));
        }
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter(Constants.LOADBALANCE_KEY, RoundRobinLoadBalance.NAME));
            registryDirectory2.notify(serviceUrls);
            
            invocation = new RpcInvocation();      
            invocation.setMethodName("get");
            invokers = registryDirectory2.list(invocation);
            
            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assert.assertEquals(LeastActiveLoadBalance.NAME,url.getMethodParameter("get",Constants.LOADBALANCE_KEY));
        }
    }
    
    /**
     * When destroying, RegistryDirectory should:
     * 1. be disconnected from Registry
     * 2. destroy all invokers
     */
    @Test
    public void testDestroy(){
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1,getXXX2,getXXX3"));
        
        registryDirectory.notify(serviceUrls);
        registryDirectory.destroy();
        Map<String, List<Invoker<RegistryDirectoryTest>>> methodInvokerMap = registryDirectory.getMethodInvokerMap();
        Map<String, Invoker<RegistryDirectoryTest>> urlInvokerMap = registryDirectory.getUrlInvokerMap();

        Assert.assertTrue(methodInvokerMap == null);
        Assert.assertEquals(0, urlInvokerMap.size());
        //List<U> urls = mockRegistry.getSubscribedUrls();
        
        RpcInvocation inv = new RpcInvocation();
        try {
            invokers = registryDirectory.list(inv);
            fail();
        } catch (RpcException e) {
            Assert.assertTrue(e.getMessage().contains("already destroyed"));
        }
        
    }
    
    @Test
    public void testDubbo1UrlWithGenericInvocation(){
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL_DUBBO_NOPATH.addParameter("methods", "getXXX1,getXXX2,getXXX3"));
        
        registryDirectory.notify(serviceUrls);
        
        //Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException;
        invocation = new RpcInvocation(Constants.$INVOKE, 
                new Class[]{String.class, String[].class, Object[].class}, 
                new Object[]{"getXXX1", "", new Object[]{}});
        
        invokers = registryDirectory.list(invocation);
        
        Assert.assertEquals(1, invokers.size());
        Assert.assertEquals("dubbo://"+NetUtils.getLocalHost()+":9092/com.alibaba.dubbo.demo.DemoService?callbacks=100&check=false&methods=getXXX1,getXXX2,getXXX3", invokers.get(0).toString());
        
    }
    
    enum Param{
      MORGAN,
    };
    
    /**
     * When the first arg of a method is String or Enum, Registry server 
     * can do parameter-value-based routing.
     */
    @Test
    public void testParmeterRoute(){
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1.napoli"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1.MORGAN,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1.morgan,getXXX2,getXXX3"));
        
        registryDirectory.notify(serviceUrls);
        
        invocation = new RpcInvocation(Constants.$INVOKE, 
                new Class[]{String.class, String[].class, Object[].class}, 
                new Object[]{"getXXX1", new String[]{"Enum"}, new Object[]{Param.MORGAN}});
        
        invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1, invokers.size());
    }
    
    /**
     * Empty notify cause forbidden, non-empty notify cancels forbidden state 
     */
    @Test
    public void testEmptyNotifyCauseForbidden(){
        invokers = null;
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        registryDirectory.notify(serviceUrls);
        
        RpcInvocation inv = new RpcInvocation();
        try{
            invokers = registryDirectory.list(inv);
        }catch(RpcException e){
            Assert.assertEquals(RpcException.FORBIDDEN_EXCEPTION, e.getCode());
        }
        
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1,getXXX2,getXXX3"));
        
        registryDirectory.notify(serviceUrls);
        inv.setMethodName("getXXX2");
        invokers = registryDirectory.list(inv);
        
        Assert.assertEquals(2, invokers.size());
    }
    
    private static boolean isScriptUnsupported = new ScriptEngineManager().getEngineByName("javascript") == null;
    /**
     * 1. notify twice, the second time notified router rules should completely replace the former one.
     * 2. notify with no router url, do nothing to current routers
     * 3. notify with only one router url, with router=clean, clear all current routers
     */
    @Test
    public void testNotifyRouterUrls(){
        if (isScriptUnsupported) return;
        
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        // without ROUTER_KEY, the first router should not be created.
        serviceUrls.add(ROUTERURL.addParameter(RpcConstants.TYPE_KEY, "javascript")
                                 .addParameter(RpcConstants.RULE_KEY, "function test1(){}"));
        serviceUrls.add(ROUTERURL2.addParameter(RpcConstants.TYPE_KEY, "javascript")
                                 .addParameter(RpcConstants.ROUTER_KEY, ScriptRouterFactory.NAME)
                                 .addParameter(RpcConstants.RULE_KEY, "function test1(){}"));

        registryDirectory.notify(serviceUrls);
        List<Router> routers = registryDirectory.getRouters();
        Assert.assertEquals(1, routers.size());
        Assert.assertEquals(ScriptRouter.class, routers.get(0).getClass());
        
        registryDirectory.notify(new ArrayList<URL>());
        routers = registryDirectory.getRouters();
        Assert.assertEquals(1, routers.size());
        Assert.assertEquals(ScriptRouter.class, routers.get(0).getClass());
        
        serviceUrls.clear();
        serviceUrls.add(ROUTERURL.addParameter(RpcConstants.ROUTER_KEY, RpcConstants.ROUTER_TYPE_CLEAR));
        registryDirectory.notify(serviceUrls);
        routers = registryDirectory.getRouters();
        Assert.assertEquals(0, routers.size());
    }
    
}