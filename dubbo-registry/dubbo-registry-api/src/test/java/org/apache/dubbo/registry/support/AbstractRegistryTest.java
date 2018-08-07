package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractRegistryTest {
    private static final Map<String, String> parametersProvider = new LinkedHashMap<>();
    private static final Map<String, String> parametersConsumer = new LinkedHashMap<>();

    static {
        parametersProvider.put("anyhost", "true");
        parametersProvider.put("application", "demo-provider");
        parametersProvider.put("dubbo", "2.0.2");
        parametersProvider.put("generic", "false");
        parametersProvider.put("interface", "org.apache.dubbo.demo.DemoService");
        parametersProvider.put("methods", "sayHello");
        parametersProvider.put("pid", "1489");
        parametersProvider.put("side", "provider");
        parametersProvider.put("timestamp", String.valueOf(System.currentTimeMillis()));

        parametersConsumer.put("application", "demo-consumer");
        parametersConsumer.put("category", "consumer");
        parametersConsumer.put("check", "false");
        parametersConsumer.put("dubbo", "2.0.2");
        parametersConsumer.put("interface", "org.apache.dubbo.demo.DemoService");
        parametersConsumer.put("methods", "sayHello");
        parametersConsumer.put("pid", "1676");
        parametersConsumer.put("qos.port", "333333");
        parametersConsumer.put("side", "consumer");
        parametersConsumer.put("timestamp", String.valueOf(System.currentTimeMillis()));
    }

    private URL mockUrl = new URL("dubbo", "127.0.0.0", 2200);
    private AbstractRegistry abstractRegistry = new AbstractRegistry(mockUrl) {
        @Override
        public boolean isAvailable() {
            return false;
        }
    };

    private List<URL> getList() {
        List<URL> list = new ArrayList<>();
        URL url_1 = new URL("dubbo", "127.0.0.0", 1000);
        URL url_2 = new URL("dubbo", "127.0.0.1", 1001);
        URL url_3 = new URL("dubbo", "127.0.0.2", 1002);
        list.add(url_1);
        list.add(url_2);
        list.add(url_3);
        return list;
    }


    /**
     * 测试注册方法
     * 传入单个url进行注册，判断该url是否在已经注册成功的set里
     * 传入url list，判断已经注册成功的set大小是否和url list大小相同
     */
    @Test
    public void testRegister() {
        //测试单个注册
        abstractRegistry.register(mockUrl);
        assert abstractRegistry.getRegistered().contains(mockUrl);

        //测试多个注册
        abstractRegistry.getRegistered().clear();
        List<URL> urlList = getList();
        for (URL url : urlList) {
            abstractRegistry.register(url);
        }
        Assert.assertThat(abstractRegistry.getRegistered().size(), Matchers.equalTo(urlList.size()));
    }

    /**
     * 测试注销注册方法
     * 传入单个url，注销注册，判断已经注册的url列表里是否包含该url
     * 传入url 列表，注销注册，判断已经注册的列表是否被清空
     */
    @Test
    public void testUnregister() {
        //测试单个注销注册
        URL url = new URL("dubbo", "127.0.0.0", 2200);
        abstractRegistry.getRegistered().add(url);
        abstractRegistry.unregister(url);
        Assert.assertThat(false, Matchers.equalTo(abstractRegistry.getRegistered().contains(url)));

        //测试重复注销
        abstractRegistry.unregister(url);
        Assert.assertThat(false,Matchers.equalTo(abstractRegistry.getRegistered().contains(url)));

        //测试多个注销注册
        abstractRegistry.getRegistered().clear();

        List<URL> urlList = getList();
        for (URL urlSub : urlList) {
            abstractRegistry.getRegistered().add(urlSub);
        }
        for (URL urlSub : urlList) {
            abstractRegistry.unregister(urlSub);
        }
        Assert.assertThat(0, Matchers.equalTo(abstractRegistry.getRegistered().size()));

    }

    /**
     * 测试订阅和取消订阅方法
     * 传入url进行订阅，判断相应的listener是否加入监听set
     * 传入url取消订阅，判断相应的listener是否从监听set移除
     */
    @Test
    public void testSubscribeAndUnsubscribe() {

        //订阅测试
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listener = urls -> notified.set(Boolean.TRUE);
        URL url = new URL("dubbo", "127.0.0.0", 2200);
        abstractRegistry.subscribe(url, listener);
        Set<NotifyListener> subscribeListeners = abstractRegistry.getSubscribed().get(url);
        Assert.assertThat(true, Matchers.equalTo(subscribeListeners.contains(listener)));

        //取消订阅测试
        abstractRegistry.unsubscribe(url, listener);
        Set<NotifyListener> unsubscribeListeners = abstractRegistry.getSubscribed().get(url);
        Assert.assertThat(false, Matchers.equalTo(unsubscribeListeners.contains(listener)));
    }

    /**
     * 测试recover()方法的逻辑正确性
     * <p>
     * ZookeeperRegistry:
     * zkClient创建成功以后，会监听RECONNECTED事件，
     * recover方法主要做一件事：将需要暴露的url放在failedRegistered（Set<URL>）中，
     * 将需要订阅的服务放在failedSubscribed（Set<URL>）中。
     * 说明RECONNECTED时，因为所有需要暴露的服务都需要重新注册，
     * 所以其实是将需要暴露、订阅的url都放到failedRegistered、failedSubscribed中。
     */
    @Test
    public void testRecover() {
        List<URL> list = getList();
        try {
            abstractRegistry.recover();
            Assert.assertEquals(0, abstractRegistry.getRegistered().size());
            for (URL url : list) {
                abstractRegistry.register(url);
            }
            Assert.assertEquals(3, abstractRegistry.getRegistered().size());
            abstractRegistry.recover();
            Assert.assertEquals(3, abstractRegistry.getRegistered().size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 测试提醒方法，传入url list
     * 判断url list是否在已经提醒的set
     */
    @Test
    public void testNotifyList() {
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listner1 = urls -> notified.set(Boolean.TRUE);
        URL url1 = new URL("dubbo", "127.0.0.0", 2200, parametersConsumer);
        abstractRegistry.subscribe(url1, listner1);

        NotifyListener listner2 = urls -> notified.set(Boolean.TRUE);
        URL url2 = new URL("dubbo", "127.0.0.1", 2201, parametersConsumer);
        abstractRegistry.subscribe(url2, listner2);

        NotifyListener listner3 = urls -> notified.set(Boolean.TRUE);
        URL url3 = new URL("dubbo", "127.0.0.2", 2202, parametersConsumer);
        abstractRegistry.subscribe(url3, listner3);

        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        abstractRegistry.notify(urls);
        Map<URL, Map<String, List<URL>>> map = abstractRegistry.getNotified();

        Assert.assertThat(true, Matchers.equalTo(map.containsKey(url1)));
        Assert.assertThat(true, Matchers.equalTo(map.containsKey(url2)));
        Assert.assertThat(true, Matchers.equalTo(map.containsKey(url3)));

    }

    /**
     * 测试提醒方法，传入url，listner，url list参数
     * 判断该url是否在已经提醒的set
     */
    @Test
    public void testNotify() {
        final AtomicReference<Boolean> notified = new AtomicReference<Boolean>(false);
        NotifyListener listner1 = urls -> notified.set(Boolean.TRUE);
        URL url1 = new URL("dubbo", "127.0.0.0", 2200, parametersConsumer);
        abstractRegistry.subscribe(url1, listner1);

        NotifyListener listner2 = urls -> notified.set(Boolean.TRUE);
        URL url2 = new URL("dubbo", "127.0.0.1", 2201, parametersConsumer);
        abstractRegistry.subscribe(url2, listner2);

        NotifyListener listner3 = urls -> notified.set(Boolean.TRUE);
        URL url3 = new URL("dubbo", "127.0.0.2", 2202, parametersConsumer);
        abstractRegistry.subscribe(url3, listner3);

        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        urls.add(url3);
        abstractRegistry.notify(url1, listner1, urls);
        Map<URL, Map<String, List<URL>>> map = abstractRegistry.getNotified();

        Assert.assertThat(true, Matchers.equalTo(map.containsKey(url1)));
        Assert.assertThat(false, Matchers.equalTo(map.containsKey(url2)));
        Assert.assertThat(false, Matchers.equalTo(map.containsKey(url3)));
    }
}
