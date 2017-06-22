package com.alibaba.dubbo.rpc.cluster.loadbalance;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.alibaba.dubbo.rpc.support.MockInvoker;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static javax.swing.UIManager.get;

/**
 * Created by wujianchao on 2017/6/7.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class WeightedConsistentHashLoadBalanceTest {

    List<Invoker<TestService>> invokers = new ArrayList<Invoker<TestService>>();

    public static final String URL_PATERN = "test://host-%d/TestService?" + Constants.WARMUP_KEY + "=%d&" + Constants.REMOTE_TIMESTAMP_KEY + "=%d";

    private int WARMUP = 2 * 60 * 1000;

    private LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(WeightedConsistentHashLoadBalance.NAME);

    private AtomicInteger HOST_ID = new AtomicInteger(0);
    private int RUN_TIMES = 1000;

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        long current = System.currentTimeMillis();
        //old providers
        for (int i = 0; i < 2; i++) {
            URL url = URL.valueOf(String.format(URL_PATERN, getHostId(), WARMUP, (current - WARMUP)));
            invokers.add(new MockInvoker<TestService>(url));
            System.out.println("Adding new service providers : " + url.toFullString());
        }
        //new provider
        for (int i = 0; i < 1; i++) {
            URL url = URL.valueOf(String.format(URL_PATERN, getHostId(), WARMUP, current));
            invokers.add(new MockInvoker<TestService>(url));
            System.out.println("Adding new service providers : " + url.toFullString());
        }
    }

    @Test
    public void isTimeToRebuildConsistentHashTest() throws Exception {
        Class WeightedConsistentHashSelectorClass = Class.forName(WeightedConsistentHashLoadBalance.class.getName()+"$WeightedConsistentHashSelector");
        Method isTimeToRebuildConsistentHash = WeightedConsistentHashSelectorClass.getDeclaredMethod("isTimeToRebuildConsistentHash", List.class);
        isTimeToRebuildConsistentHash.setAccessible(true);
        Object selector = WeightedConsistentHashSelectorClass.getDeclaredConstructors()[0]
                .newInstance(lb, invokers, getInvocation(), System.identityHashCode(invokers));

        boolean flag = flag = (Boolean) isTimeToRebuildConsistentHash.invoke(selector, invokers);
        Assert.assertFalse(flag);

        Thread.sleep(10*1000);
        flag = (Boolean) isTimeToRebuildConsistentHash.invoke(selector, invokers);
        Assert.assertFalse(flag);

        Thread.sleep(60*1000);
        flag = (Boolean) isTimeToRebuildConsistentHash.invoke(selector, invokers);
        Assert.assertTrue(flag);

        Thread.sleep(60*1000);
        flag = (Boolean) isTimeToRebuildConsistentHash.invoke(selector, invokers);
        Assert.assertTrue(flag);

        flag = (Boolean) isTimeToRebuildConsistentHash.invoke(selector, invokers);
        Assert.assertFalse(flag);

    }

    @Test
    public void buildConsistentHashTest() throws Exception {
        Class WeightedConsistentHashSelectorClass = Class.forName(WeightedConsistentHashLoadBalance.class.getName()+"$WeightedConsistentHashSelector");
        Method buildConsistentHash = WeightedConsistentHashSelectorClass.getDeclaredMethod("buildConsistentHash", List.class, Invocation.class);
        buildConsistentHash.setAccessible(true);
        Field virtualInvokersField = WeightedConsistentHashSelectorClass.getDeclaredField("virtualInvokers");
        virtualInvokersField.setAccessible(true);

        Object selector = WeightedConsistentHashSelectorClass.getDeclaredConstructors()[0]
                .newInstance(lb, invokers, getInvocation(), System.identityHashCode(invokers));

        Thread.sleep(10);
        buildConsistentHash.invoke(selector, invokers, getInvocation());
        TreeMap<Long, Invoker> virtualInvokers = (TreeMap<Long, Invoker>) virtualInvokersField.get(selector);
        Map<Invoker, Long> counter = countInvokerSize(virtualInvokers);
        Assert.assertEquals(counter.get(invokers.get(0)), counter.get(invokers.get(1)));
        Assert.assertTrue(counter.get(invokers.get(0)) > counter.get(invokers.get(2)));

        Thread.sleep(WARMUP + 10* 1000);
        buildConsistentHash.invoke(selector, invokers, getInvocation());
        virtualInvokers = (TreeMap<Long, Invoker>) virtualInvokersField.get(selector);
        counter = countInvokerSize(virtualInvokers);
        Assert.assertEquals(counter.get(invokers.get(0)), counter.get(invokers.get(1)));
        Assert.assertEquals(counter.get(invokers.get(0)), counter.get(invokers.get(2)));
    }

    private Map<Invoker, Long>  countInvokerSize(TreeMap<Long, Invoker> virtualInvokers){
        Map<Invoker, Long> counter = new HashMap<Invoker, Long>();
        for (Map.Entry<Long, Invoker> entry : virtualInvokers.entrySet()) {
            if(counter.get(entry.getValue()) == null){
                counter.put(entry.getValue(), 1L);
            }else{
                counter.put(entry.getValue(), counter.get(entry.getValue()) + 1);
            }
        }
        System.out.println(counter);
        return counter;
    }

    private int getHostId() {
        return HOST_ID.getAndIncrement();
    }


    /**
     * new an invocation of different arguments
     *
     * @return invocation
     */
    public Invocation getInvocation() {
        return new Invocation() {
            public String getMethodName() {
                return "doSomething";
            }

            public Class<?>[] getParameterTypes() {
                return new Class[]{String.class, String.class};
            }

            public Object[] getArguments() {
                return new String[]{"arg1" + new Random().nextFloat(), "arg2" + new Random().nextFloat()};
            }


            public Map<String, String> getAttachments() {
                return null;
            }

            public String getAttachment(String key) {
                return null;
            }

            public String getAttachment(String key, String defaultValue) {
                return null;
            }

            public Invoker<?> getInvoker() {
                return null;
            }
        };
    }

}
