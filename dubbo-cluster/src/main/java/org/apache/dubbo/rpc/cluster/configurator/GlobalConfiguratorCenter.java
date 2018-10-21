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
package org.apache.dubbo.rpc.cluster.configurator;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ChildListener;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.rpc.Invoker;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GlobalConfiguratorCenter
 *
 */
public class GlobalConfiguratorCenter {
    private  static final ConcurrentHashMap<String,URL> configuratorMap= new ConcurrentHashMap();
    private  static   ZookeeperClient zkClient = null;

    public static void initZkClient( ZookeeperClient zookeeperClient){
        zkClient=zookeeperClient;
        zkClient.addChildListener( Constants.GLOBALCONFIG_ZK_PATH, new ChildListener()  {
            @Override
            public void childChanged(String path, List<String> children) {
                for (String configUrl: children) {
                        URL url = URL.valueOf( URL.decode(configUrl));
                        String mapKey=url.getAddress();
                        if(mapKey!=null&&!"".equals(mapKey)) configuratorMap.put(mapKey,url);
                }
            }
        });
    }



    public  static  int getInvokerWeight(Invoker<?> invoker, int deafultWeight) {
        try {

            if(invoker!=null&& invoker.getUrl()!=null){
                URL url =  configuratorMap.get(invoker.getUrl().getAddress());
                if(url!=null)deafultWeight =url.getParameter( Constants.WEIGHT_KEY, deafultWeight);
                return  deafultWeight;
            }
        }catch (Exception e){
            return  deafultWeight;
        }
        return  deafultWeight;
    }



}
