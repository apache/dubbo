package com.alibaba.dubbo.rpc.benchmark;

import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;

/**
 * Abstract Service Factory,create custom nums Service
 * 
 * @author tony.chenl
 */
public class ServiceFactory<T> {

    String targetIP       = null;

    int    targetPort     = 0;

    int    connectTimeout = 0;

    int    clientNums     = 0;

    public String getTargetIP() {
        return targetIP;
    }

    public void setTargetIP(String targetIP) {
        this.targetIP = targetIP;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getClientNums() {
        return clientNums;
    }

    public void setClientNums(int clientNums) {
        this.clientNums = clientNums;
    }

    // Cache ExchangeClient
    private static ConcurrentHashMap<String, Object> services = new ConcurrentHashMap<String, Object>();

    @SuppressWarnings("unchecked")
    public T get(final Class<T> cls){
        String key = cls.getName();
        if (services.containsKey(key)) {
            return (T) services.get(key);

        } else {
            T service = createClient(cls, targetIP, targetPort, connectTimeout,clientNums);
            services.put(key, service);
            return (T) services.get(key);
        }
    }

    protected T createClient(Class<T> cls, String targetIP, int targetPort, int connectTimeout,int clientNums){
        ReferenceConfig<T> referenceConfig = new ReferenceConfig<T>();
        referenceConfig.setInterface(cls);
        StringBuilder url = new StringBuilder();
        url.append("dubbo://");
        url.append(targetIP);
        url.append(":");
        url.append(targetPort);
        url.append("/");
        url.append(cls.getName());
        referenceConfig.setUrl(url.toString());
        // hardcode
        referenceConfig.setConnections(clientNums);
        ApplicationConfig application = new ApplicationConfig();
        application.setName("dubbo_consumer");
        referenceConfig.setApplication(application);
        referenceConfig.setTimeout(connectTimeout);
        return referenceConfig.get();
    }

}
