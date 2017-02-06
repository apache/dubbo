package com.alibaba.dubbo.rpc.protocol.jms;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.config.spring.util.SpringUtil;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.remoting.JmsInvokerProxyFactoryBean;
import org.springframework.jms.remoting.JmsInvokerServiceExporter;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2017/2/5.
 */
public class JmsProtocol extends AbstractProxyProtocol {

    private final Map<String, ConnectionFactory> connectionFactoryMap = new ConcurrentHashMap<>();

    @Override
    public int getDefaultPort() {
        return 61617;
    }

    /**
     * {"methodName":"sayHello","parameterTypes":["java.lang.String"],"arguments":["wuyu"]}
     * json调用方式 message类型为 ActiveMQBytesMessage properties 属性 typeIdPropertyName = 接口名(例com.alibaba.dubbo.demo.JmsService)
     *
     * @param impl
     * @param type
     * @param url
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    protected <T> Runnable doExport(T impl, final Class<T> type, URL url) throws RpcException {
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        String queueName = url.getParameter("version", "0.0.0.") + url.getParameter("group", "defaultGroup.") + type.getName();
        ConnectionFactory connectionFactory = getConnectionFactory(url, threads);
        JmsInvokerServiceExporter exporter = new JmsInvokerServiceExporter();
        exporter.setServiceInterface(type);
        exporter.setService(impl);
        exporter.setBeanClassLoader(JmsProtocol.class.getClassLoader());
        exporter.afterPropertiesSet();
        final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setMessageListener(exporter);
        container.setConnectionFactory(connectionFactory);
        container.setDestination(getQueue(queueName));
        container.afterPropertiesSet();
        container.start();
        return new Runnable() {
            @Override
            public void run() {
                container.destroy();
            }
        };
    }

    @Override
    protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
        final int timeout = url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        final int connections = url.getParameter(Constants.CONNECTIONS_KEY, 20);
        String queueName = url.getParameter("version", "0.0.0.") + url.getParameter("group", "defaultGroup.") + type.getName();

        JmsInvokerProxyFactoryBean factoryBean = new JmsInvokerProxyFactoryBean();
        factoryBean.setServiceInterface(type);
        factoryBean.setQueue(getQueue(queueName));
        factoryBean.setReceiveTimeout(timeout);
        factoryBean.setConnectionFactory(getConnectionFactory(url, connections));
        factoryBean.afterPropertiesSet();
        return (T) factoryBean.getObject();
    }

    public ConnectionFactory getConnectionFactory(URL url, int threads) {
        String addr = url.getHost() + ":" + url.getPort();
        ConnectionFactory connectionFactory = connectionFactoryMap.get(addr);
        if (connectionFactory == null) {
            Set<String> beanNamesForType = SpringUtil.getBeanNamesForType(ConnectionFactory.class);
            if (beanNamesForType.size() > 0) {
                connectionFactory = SpringUtil.getBean(ConnectionFactory.class);
            } else {
                String username = ConfigUtils.getProperty("username");
                String password = ConfigUtils.getProperty("password");
                String brokerURL = ConfigUtils.getProperty("brokerURL");
                if (brokerURL == null) {
                    brokerURL = ActiveMQConnectionFactory.DEFAULT_BROKER_URL;
                }
                ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(username, password, brokerURL);
                activeMQConnectionFactory.setMaxThreadPoolSize(threads);
                connectionFactory = activeMQConnectionFactory;
            }
        }
        return connectionFactory;
    }

    public Queue getQueue(String name) {
        String queue = ConfigUtils.getProperty("queue");
        if (queue != null) {
            try {
                Class<?> aClass = Class.forName(queue);
                Constructor<?> constructor = aClass.getConstructor(String.class);
                return (Queue) constructor.newInstance(name);
            } catch (Exception e) {
                throw new RpcException(e);
            }
        }
        return new ActiveMQQueue(name);

    }

    @Override
    public void destroy() {
        connectionFactoryMap.clear();
    }
}
