/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.model;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.rpc.Invoker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// TODO need to adjust project structure in order to fully utilize the methods introduced here.
public class ApplicationModel {

    protected static final Logger logger = LoggerFactory.getLogger(ApplicationModel.class);

    /**
     * full qualified class name -> provided service
     */
    private static final ConcurrentMap<String, ProviderModel> providedServices = new ConcurrentHashMap<String, ProviderModel>();
    /**
     * full qualified class name -> subscribe service
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

    public static boolean initConsumerModel(String serviceName, ConsumerModel consumerModel) {
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
