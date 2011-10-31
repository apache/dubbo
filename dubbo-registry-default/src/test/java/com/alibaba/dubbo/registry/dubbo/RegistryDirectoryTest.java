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

import java.lang.reflect.Field;
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
    RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
    Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    
    static {
        SimpleRegistryExporter.exportIfAbsent(9090);
        SimpleRegistryExporter.exportIfAbsent(9091);
        SimpleRegistryExporter.exportIfAbsent(9092);
        SimpleRegistryExporter.exportIfAbsent(9093);
    }
    private static String service = DemoService.class.getName();
    RpcInvocation invocation = new RpcInvocation();
    URL url = REGURL.addParameter("test", "reg");
    URL url2 = REGURL.addParameter("test", "reg").addParameterAndEncoded(RpcConstants.REFER_KEY, 
            "key=query&" + Constants.LOADBALANCE_KEY + "=" + LeastActiveLoadBalance.NAME);
    public static URL  REGURL= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9090/"+service);
    public static URL  SERVICEURL= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9091/"+service);
    public static URL  SERVICEURL2= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9092/"+service);
    public static URL  SERVICEURL3= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9093/"+service);
    public static URL  SERVICEURL_DUBBO_NOPATH= URL.valueOf("dubbo://"+NetUtils.getLocalHost()+":9092");
    
    RegistryDirectory<RegistryDirectoryTest> registryDirectory = new RegistryDirectory(RegistryDirectoryTest.class, url);
    {
        registryDirectory.setRegistry(registryFactory.getRegistry(url));
        registryDirectory.setProtocol(protocol);
    }
    
    
    @Test
    public void test_Constructor_WithErrorParam(){
        try{
            new RegistryDirectory(null, null);
        fail();
        }catch (IllegalArgumentException e) {
            
        }
        try{
            new RegistryDirectory(null, url2);
        fail();
        }catch (IllegalArgumentException e) {
            
        }
        try{
            new RegistryDirectory(RegistryDirectoryTest.class, URL.valueOf("dubbo://10.20.30.40:9090"));
            fail();
        }catch (IllegalArgumentException e) {
            
        }
    }
    @Test
    public void test_Constructor_CheckStatus() throws Exception{
        URL url = URL.valueOf("registry://10.20.30.40/"+service+"?a=b").addParameterAndEncoded(RpcConstants.REFER_KEY, "foo=bar");
        RegistryDirectory<RegistryDirectoryTest> reg = new RegistryDirectory(RegistryDirectoryTest.class, url);
        Field field = reg.getClass().getDeclaredField("queryMap");
        field.setAccessible(true);
        Map<String, String> queryMap = (Map<String, String>)field.get(reg);
        Assert.assertEquals("bar", queryMap.get("foo"));
        Assert.assertEquals(url.removeParameter(RpcConstants.REFER_KEY), reg.getUrl());
    }
    
    @Test
    public void testNotified_Normal() {
        List invokers = registryDirectory.list(invocation);
        Assert.assertEquals(0,invokers.size());
        Assert.assertEquals(false, registryDirectory.isAvailable());
        
        test_Notified2invokers();
        test_Notified1invokers();
        test_Notified3invokers();
        testforbid();
    }
    @Test
    public void testNotified_WithError() {
        List<URL> serviceUrls = new ArrayList<URL> ();
        //ignore error log
        URL badurl = URL.valueOf("notsupported://"+NetUtils.getLocalHost()+"/"+service);
        serviceUrls.add(badurl);
        serviceUrls.add(SERVICEURL);

        registryDirectory.notify(serviceUrls);
        Assert.assertEquals(true, registryDirectory.isAvailable());
        List invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1, invokers.size());
    }
    
    @Test
    public void testNotified_WithDuplicateUrls() {
        List<URL> serviceUrls = new ArrayList<URL> ();
        //ignore error log
        serviceUrls.add(SERVICEURL);
        serviceUrls.add(SERVICEURL);

        registryDirectory.notify(serviceUrls);
        List invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1, invokers.size());
        
    }
    
    
    //forbid
    private void testforbid(){
        invocation = new RpcInvocation();
        List<URL> serviceUrls = new ArrayList<URL> ();
        registryDirectory.notify(serviceUrls);
        Assert.assertEquals("invokers size=0 ,then the registry directory is not available", false, registryDirectory.isAvailable());
        try {
            registryDirectory.list(invocation);
            fail("forbid must throw RpcException");
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.FORBIDDEN_EXCEPTION, e.getCode());
        }
      }
    
    //notify one invoker
    private void test_Notified1invokers(){
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));//.addParameter("refer.autodestroy", "true")
        registryDirectory.notify(serviceUrls);
        Assert.assertEquals(true, registryDirectory.isAvailable());

        invocation = new RpcInvocation();
        
        List invokers = registryDirectory.list(invocation);
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
    private void test_Notified2invokers(){
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));

        registryDirectory.notify(serviceUrls);
        Assert.assertEquals(true, registryDirectory.isAvailable());
      
        
        invocation = new RpcInvocation();
        
        List invokers = registryDirectory.list(invocation);
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
    private void test_Notified3invokers(){
        List<URL> serviceUrls = new ArrayList<URL> ();
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1,getXXX2,getXXX3"));

        registryDirectory.notify(serviceUrls);
        Assert.assertEquals(true, registryDirectory.isAvailable());
        
        invocation = new RpcInvocation();
           
        List invokers = registryDirectory.list(invocation);
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
        
        RegistryDirectory<RegistryDirectoryTest> registryDirectory2 = new RegistryDirectory(RegistryDirectoryTest.class, url2);
        {
            registryDirectory2.setRegistry(registryFactory.getRegistry(url2));
            registryDirectory2.setProtocol(protocol);
        }
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        //检验注册中心的参数需要被清除
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
            registryDirectory.notify(serviceUrls);
            
            invocation = new RpcInvocation();        
            List invokers = registryDirectory.list(invocation);
            
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
            List invokers = registryDirectory.list(invocation);
            
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
            List invokers = registryDirectory2.list(invocation);
            
            Invoker invoker = (Invoker) invokers.get(0);
            URL url = invoker.getUrl();
            Assert.assertEquals("query",url.getParameter("key"));
        }
        
        {
            serviceUrls.clear();
            serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
            registryDirectory.notify(serviceUrls);
            
            invocation = new RpcInvocation();        
            List invokers = registryDirectory.list(invocation);
            
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
            List invokers = registryDirectory2.list(invocation);
            
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
        Assert.assertEquals(false, registryDirectory.isAvailable());
        registryDirectory.destroy();
        Map<String, List<Invoker<RegistryDirectoryTest>>> methodInvokerMap = registryDirectory.getMethodInvokerMap();
        Map<String, Invoker<RegistryDirectoryTest>> urlInvokerMap = registryDirectory.getUrlInvokerMap();

        Assert.assertTrue(methodInvokerMap == null);
        Assert.assertEquals(0, urlInvokerMap.size());
        //List<U> urls = mockRegistry.getSubscribedUrls();
        
        RpcInvocation inv = new RpcInvocation();
        try {
            registryDirectory.list(inv);
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
        
        List invokers = registryDirectory.list(invocation);
        
        Assert.assertEquals(1, invokers.size());
        Assert.assertEquals("dubbo://"+NetUtils.getLocalHost()+":9092/"+service+"?check=false&methods=getXXX1,getXXX2,getXXX3", invokers.get(0).toString());
        
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
        
        List invokers = registryDirectory.list(invocation);
        Assert.assertEquals(1, invokers.size());
    }
    
    /**
     * Empty notify cause forbidden, non-empty notify cancels forbidden state 
     */
    @Test
    public void testEmptyNotifyCauseForbidden(){
        List invokers = null;
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        registryDirectory.notify(serviceUrls);
        
        RpcInvocation inv = new RpcInvocation();
        try{
            invokers = registryDirectory.list(inv);
        }catch(RpcException e){
            Assert.assertEquals(RpcException.FORBIDDEN_EXCEPTION, e.getCode());
            Assert.assertEquals(false, registryDirectory.isAvailable());
        }
        
        serviceUrls.add(SERVICEURL.addParameter("methods", "getXXX1"));
        serviceUrls.add(SERVICEURL2.addParameter("methods", "getXXX1,getXXX2"));
        serviceUrls.add(SERVICEURL3.addParameter("methods", "getXXX1,getXXX2,getXXX3"));
        
        registryDirectory.notify(serviceUrls);
        inv.setMethodName("getXXX2");
        invokers = registryDirectory.list(inv);
        Assert.assertEquals(true, registryDirectory.isAvailable());
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
        URL  routerurl= URL.valueOf(RpcConstants.ROUTE_PROTOCOL + "://"+NetUtils.getLocalHost()+":9096/");
        URL  routerurl2= URL.valueOf(RpcConstants.ROUTE_PROTOCOL + "://"+NetUtils.getLocalHost()+":9097/");
        
        List<URL> serviceUrls = new ArrayList<URL> ();
        // without ROUTER_KEY, the first router should not be created.
        serviceUrls.add(routerurl.addParameter(RpcConstants.TYPE_KEY, "javascript")
                                 .addParameter(RpcConstants.RULE_KEY, "function test1(){}"));
        serviceUrls.add(routerurl2.addParameter(RpcConstants.TYPE_KEY, "javascript")
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
        serviceUrls.add(routerurl.addParameter(RpcConstants.ROUTER_KEY, RpcConstants.ROUTER_TYPE_CLEAR));
        registryDirectory.notify(serviceUrls);
        routers = registryDirectory.getRouters();
        Assert.assertEquals(0, routers.size());
    }
    
    
    private static interface DemoService {}
}