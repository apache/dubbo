package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.registry.integration.RegistryDirectory;
import com.alibaba.dubbo.rpc.Invoker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author qinliujie
 * @date 2017/11/23
 */
public class ProviderConsumerRegTable {
    public static ConcurrentHashMap<String, Set<ProviderInvokerWrapper>> providerInvokers = new ConcurrentHashMap<String, Set<ProviderInvokerWrapper>>();
    public static ConcurrentHashMap<String, Set<ConsumerInvokerWrapper>> consumerInvokers = new ConcurrentHashMap<String, Set<ConsumerInvokerWrapper>>();

    public static void registerProvider(Invoker invoker, URL registryUrl, URL providerUrl) {
        ProviderInvokerWrapper wrapperInvoker = new ProviderInvokerWrapper(invoker, registryUrl, providerUrl);
        String serviceName = providerUrl.getServiceInterface();
        String version = providerUrl.getParameter(Constants.VERSION_KEY);
        String serviceUniqueName = serviceName + ":" + version;
        Set<ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            providerInvokers.putIfAbsent(serviceUniqueName, new ConcurrentHashSet<ProviderInvokerWrapper>());
            invokers = providerInvokers.get(serviceUniqueName);
        }
        invokers.add(wrapperInvoker);
    }

    public static Set<ProviderInvokerWrapper> getProviderInvoker(String serviceUniqueName) {
        Set<ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            return Collections.emptySet();
        }
        return invokers;
    }

    public static ProviderInvokerWrapper getWrapper(Invoker invoker) {
        URL providerUrl = invoker.getUrl();
        String serviceName = providerUrl.getServiceInterface();
        String version = providerUrl.getParameter(Constants.VERSION_KEY);
        String serviceUniqueName = serviceName + ":" + version;
        Set<ProviderInvokerWrapper> invokers = providerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            return null;
        }

        for (ProviderInvokerWrapper providerWrapper : invokers) {
            Invoker providerInvoker = providerWrapper.getInvoker();
            if (providerInvoker == invoker) {
                return providerWrapper;
            }
        }

        return null;
    }

    public static void registerConsuemr(Invoker invoker, URL registryUrl, URL consumerUrl,RegistryDirectory registryDirectory) {
        ConsumerInvokerWrapper wrapperInvoker = new ConsumerInvokerWrapper(invoker,registryUrl,consumerUrl,registryDirectory);
        String serviceName = consumerUrl.getServiceInterface();
        String version = consumerUrl.getParameter(Constants.VERSION_KEY);
        String serviceUniqueName = serviceName + ":" + version;
        Set<ConsumerInvokerWrapper> invokers = consumerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            consumerInvokers.putIfAbsent(serviceUniqueName, new ConcurrentHashSet<ConsumerInvokerWrapper>());
            invokers = consumerInvokers.get(serviceUniqueName);
        }
        invokers.add(wrapperInvoker);
    }

    public static Set<ConsumerInvokerWrapper> getConsumerInvoker(String serviceUniqueName) {
        Set<ConsumerInvokerWrapper> invokers = consumerInvokers.get(serviceUniqueName);
        if (invokers == null) {
            return Collections.emptySet();
        }
        return invokers;
    }

}
