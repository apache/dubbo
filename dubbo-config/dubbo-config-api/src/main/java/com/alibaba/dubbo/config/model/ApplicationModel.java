package com.alibaba.dubbo.config.model;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.rpc.Invoker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author qinliujie
 * @date 2017/11/22
 */

//TODO 需要调整项目结构，才能使用后面的方法
public class ApplicationModel {

    protected static final Logger logger = LoggerFactory.getLogger(ApplicationModel.class);

    /**
     * 全限定名到提供的服务
     */
    private static final ConcurrentMap<String, ProviderModel> providedServices = new ConcurrentHashMap<String, ProviderModel>();
    /**
     * 全限定名到订阅的服务
     */
    private static final ConcurrentMap<String, ConsumerModel> consumedServices = new ConcurrentHashMap<String, ConsumerModel>();

    public static final ConcurrentMap<String, Set<Invoker>> providedServicesInvoker = new ConcurrentHashMap<String, Set<Invoker>>();

    public static List<ConsumerModel> allConsumerModels() {
        return new ArrayList<ConsumerModel>(consumedServices.values());
    }

    public static ProviderModel getProviderModel(String serviceName) {
        return providedServices.get(serviceName);
    }

    public static ConsumerModel getConsumerModel(String serviceName) {
        return consumedServices.get(serviceName);
    }

    public static List<ProviderModel> allProviderModels() {
        return new ArrayList<ProviderModel>(providedServices.values());
    }

    public static boolean initConsumerModele(String serviceName, ConsumerModel consumerModel) {
        if (consumedServices.putIfAbsent(serviceName, consumerModel) != null) {
            logger.warn("Already register the same consumer:" + serviceName);
            return false;
        }
        return true;
    }

    public static void initProviderModel(String serviceName, ProviderModel providerModel) {
        if (providedServices.put(serviceName, providerModel) != null) {
            logger.warn("already register the provider service: " + serviceName);
            return;
        }
    }

   public static void addProviderInvoker(String serviceName,Invoker invoker){
       Set<Invoker> invokers = providedServicesInvoker.get(serviceName);
       if (invokers == null){
           providedServicesInvoker.putIfAbsent(serviceName,new ConcurrentHashSet<Invoker>());
           invokers = providedServicesInvoker.get(serviceName);
       }
       invokers.add(invoker);
   }

   public Set<Invoker> getProviderInvoker(String serviceName){
       Set<Invoker> invokers = providedServicesInvoker.get(serviceName);
       if (invokers == null){
           return Collections.emptySet();
       }
       return invokers;
   }
}
