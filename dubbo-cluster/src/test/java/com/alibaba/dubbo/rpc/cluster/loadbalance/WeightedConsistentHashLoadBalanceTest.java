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

/**
 * Created by wujianchao on 2017/6/7.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class WeightedConsistentHashLoadBalanceTest {

    public static final String URL_PATTERN = "test://host-%d/TestService?"
            + Constants.WEIGHT_KEY + "=%d&"
            + Constants.WARMUP_KEY + "=%d&"
            + Constants.REMOTE_TIMESTAMP_KEY + "=%d";

    private int WARMUP = 2 * 60 * 1000;

    private LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(WeightedConsistentHashLoadBalance.NAME);

    private AtomicInteger HOST_ID = new AtomicInteger(0);
    private int RUN_TIMES = 1000;

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

    }

    private List<Invoker<TestService>> generateProviders(int weight){
        List<Invoker<TestService>> result = new ArrayList<Invoker<TestService>>();
        long current = System.currentTimeMillis();
        //stable providers
        for (int i = 0; i < 2; i++) {
            URL url = URL.valueOf(String.format(URL_PATTERN, getHostId(), weight, WARMUP, (current - WARMUP)));
            result.add(new MockInvoker<TestService>(url));
            System.out.println("Stable service providers : " + url.toFullString());
        }
        //new provider double weight
        for (int i = 0; i < 1; i++) {
            URL url = URL.valueOf(String.format(URL_PATTERN, getHostId(), 2 * weight, WARMUP, current));
            result.add(new MockInvoker<TestService>(url));
            System.out.println("Adding new service providers : " + url.toFullString());
        }
        return result;
    }

    @Test
    public void isTimeToRebuildConsistentHashTest() throws Exception {
        Class WeightedConsistentHashSelectorClass = Class.forName(WeightedConsistentHashLoadBalance.class.getName()+"$WeightedConsistentHashSelector");
        Method isTimeToRebuildConsistentHash = WeightedConsistentHashSelectorClass.getDeclaredMethod("isTimeToRebuildConsistentHash", List.class);
        isTimeToRebuildConsistentHash.setAccessible(true);

        List<Invoker<TestService>> invokers = generateProviders(100);

        Object selector = WeightedConsistentHashSelectorClass.getDeclaredConstructors()[0]
                .newInstance(lb, invokers, getInvocation(), System.identityHashCode(invokers));

        boolean flag = (Boolean) isTimeToRebuildConsistentHash.invoke(selector, invokers);
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
    public void isTimeToRebuildConsistentHashTestWhenNoRequestInWarmupTime() throws Exception {
        Class WeightedConsistentHashSelectorClass = Class.forName(WeightedConsistentHashLoadBalance.class.getName()+"$WeightedConsistentHashSelector");
        Method isTimeToRebuildConsistentHash = WeightedConsistentHashSelectorClass.getDeclaredMethod("isTimeToRebuildConsistentHash", List.class);
        isTimeToRebuildConsistentHash.setAccessible(true);

        List<Invoker<TestService>> invokers = generateProviders(100);

        Object selector = WeightedConsistentHashSelectorClass.getDeclaredConstructors()[0]
                .newInstance(lb, invokers, getInvocation(), System.identityHashCode(invokers));

        boolean flag = (Boolean) isTimeToRebuildConsistentHash.invoke(selector, invokers);
        Assert.assertFalse(!flag);

        Thread.sleep(130*1000);
        flag = (Boolean) isTimeToRebuildConsistentHash.invoke(selector, invokers);
        Assert.assertFalse(flag);

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

        List<Invoker<TestService>> invokers = generateProviders(100);

        Object selector = WeightedConsistentHashSelectorClass.getDeclaredConstructors()[0]
                .newInstance(lb, invokers, getInvocation(), System.identityHashCode(invokers));

        Thread.sleep(10000);
        buildConsistentHash.invoke(selector, invokers, getInvocation());
        TreeMap<Long, Invoker> virtualInvokers = (TreeMap<Long, Invoker>) virtualInvokersField.get(selector);
        Map<Invoker, Long> counter = countInvokerSize(virtualInvokers);
        Assert.assertTrue(counter.get(invokers.get(0)) == invokers.get(0).getUrl().getParameter(Constants.WEIGHT_KEY, 0L));
        Assert.assertTrue(counter.get(invokers.get(1)) == invokers.get(1).getUrl().getParameter(Constants.WEIGHT_KEY, 0L));
        Assert.assertTrue(counter.get(invokers.get(2)) < invokers.get(2).getUrl().getParameter(Constants.WEIGHT_KEY, 0L));

        Thread.sleep(WARMUP + 10* 1000);
        buildConsistentHash.invoke(selector, invokers, getInvocation());
        virtualInvokers = (TreeMap<Long, Invoker>) virtualInvokersField.get(selector);
        counter = countInvokerSize(virtualInvokers);

        Assert.assertTrue(counter.get(invokers.get(0)) == invokers.get(0).getUrl().getParameter(Constants.WEIGHT_KEY, 0L));
        Assert.assertTrue(counter.get(invokers.get(1)) == invokers.get(1).getUrl().getParameter(Constants.WEIGHT_KEY, 0L));
        Assert.assertTrue(counter.get(invokers.get(2)) == invokers.get(2).getUrl().getParameter(Constants.WEIGHT_KEY, 0L));
    }

    @Test
    public void buildConsistentHashTestWhenTotalWeightLargerThan1000() throws Exception {
        Class WeightedConsistentHashSelectorClass = Class.forName(WeightedConsistentHashLoadBalance.class.getName()+"$WeightedConsistentHashSelector");
        Method buildConsistentHash = WeightedConsistentHashSelectorClass.getDeclaredMethod("buildConsistentHash", List.class, Invocation.class);
        buildConsistentHash.setAccessible(true);
        Field virtualInvokersField = WeightedConsistentHashSelectorClass.getDeclaredField("virtualInvokers");
        virtualInvokersField.setAccessible(true);

        List<Invoker<TestService>> invokers = generateProviders(1000);

        Object selector = WeightedConsistentHashSelectorClass.getDeclaredConstructors()[0]
                .newInstance(lb, invokers, getInvocation(), System.identityHashCode(invokers));

        Thread.sleep(10000);
        buildConsistentHash.invoke(selector, invokers, getInvocation());
        TreeMap<Long, Invoker> virtualInvokers = (TreeMap<Long, Invoker>) virtualInvokersField.get(selector);
        Map<Invoker, Long> counter = countInvokerSize(virtualInvokers);
        //所有invoker的权重综合是4000，weightFactor=4
        Assert.assertTrue(counter.get(invokers.get(0)) == invokers.get(0).getUrl().getParameter(Constants.WEIGHT_KEY, 0L)/4);
        Assert.assertTrue(counter.get(invokers.get(1)) == invokers.get(1).getUrl().getParameter(Constants.WEIGHT_KEY, 0L)/4);
        Assert.assertTrue(counter.get(invokers.get(2)) < invokers.get(2).getUrl().getParameter(Constants.WEIGHT_KEY, 0L)/4);

        Thread.sleep(WARMUP + 10* 1000);
        buildConsistentHash.invoke(selector, invokers, getInvocation());
        virtualInvokers = (TreeMap<Long, Invoker>) virtualInvokersField.get(selector);
        counter = countInvokerSize(virtualInvokers);

        Assert.assertTrue(counter.get(invokers.get(0)) == invokers.get(0).getUrl().getParameter(Constants.WEIGHT_KEY, 0L)/4);
        Assert.assertTrue(counter.get(invokers.get(1)) == invokers.get(1).getUrl().getParameter(Constants.WEIGHT_KEY, 0L)/4);
        Assert.assertTrue(counter.get(invokers.get(2)) == invokers.get(2).getUrl().getParameter(Constants.WEIGHT_KEY, 0L)/4);
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

    @Test
    public void testWeightedConsistentHashLoadBalance() throws InterruptedException {

        Map<Invoker, AtomicLong> counter = run(generateProviders(100), RUN_TIMES);

        System.out.println("\nExecute info :");
        for (Map.Entry<Invoker, AtomicLong> entry : counter.entrySet()) {
            System.out.println(entry.getKey().getUrl().getHost().toString() + "=" + entry.getValue());
        }
    }

    public Map<Invoker, AtomicLong> run(final List<Invoker<TestService>> invokers, int times) throws InterruptedException {
        final Map<Invoker, AtomicLong> counter = new ConcurrentHashMap<Invoker, AtomicLong>();
        for (Invoker i : invokers) {
            counter.put(i, new AtomicLong(0));
        }

        boolean newInvokerAdded = false;

        //过了warmup后新增一个service provider
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                    System.out.println("New service provider up.");
                    addInvoker(invokers, counter);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //模拟长时间没有请求场景
        Thread.sleep(WARMUP + 5000);

        for (int i = 0; i < times; i++) {
            Invoker invoker = lb.select(invokers, invokers.get(0).getUrl(), getInvocation());
            counter.get(invoker).incrementAndGet();
            Thread.sleep(200);
        }

        return counter;
    }

    private void addInvoker(List<Invoker<TestService>> invokers, Map<Invoker, AtomicLong> counter) {
        URL url = URL.valueOf(String.format(URL_PATTERN, getHostId(), 100, WARMUP, System.currentTimeMillis()));
        Invoker<TestService> invoker = new MockInvoker<TestService>(url);
        invokers.add(invoker);
        counter.put(invoker, new AtomicLong(0));
    }

}
