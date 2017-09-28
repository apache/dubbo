/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.rpc.cluster.configurator;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.cluster.Configurator;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * AbstractOverrideConfigurator
 *
 * @author william.liangf
 */
public abstract class AbstractConfigurator implements Configurator {

    private final URL configuratorUrl;

    public AbstractConfigurator(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("configurator url == null");
        }
        this.configuratorUrl = url;
    }

    public static void main(String[] args) {
        System.out.println(URL.encode("timeout=100"));
    }

    public URL getUrl() {
        return configuratorUrl;
    }

    public URL configure(URL url) {
        if (configuratorUrl == null || configuratorUrl.getHost() == null
                || url == null || url.getHost() == null) {
            return url;
        }
        if (configuratorUrl.getPort() != 0) {// override输入提供端地址，意图是控制提供者机器。可能在提供端生效 也可能在消费端生效
            if (url.getPort() == configuratorUrl.getPort()) {
                return configureIfMatch(url.getHost(), url);
            }
        } else {// 没有端口，override输入消费端地址 或者 0.0.0.0
            // 1.如果是消费端地址，则意图是控制消费者机器，必定在消费端生效，提供端忽略；
            // 2.如果是0.0.0.0可能是控制提供端，也可能是控制提供端
            if (url.getParameter(Constants.SIDE_KEY, Constants.PROVIDER).equals(Constants.CONSUMER)) {
                return configureIfMatch(NetUtils.getLocalHost(), url);// NetUtils.getLocalHost是消费端注册到zk的消费者地址
            } else if (url.getParameter(Constants.SIDE_KEY, Constants.CONSUMER).equals(Constants.PROVIDER)) {
                return configureIfMatch(Constants.ANYHOST_VALUE, url);//控制所有提供端，地址必定是0.0.0.0，否则就要配端口从而执行上面的if分支了
            }
        }
        return url;
    }

    private URL configureIfMatch(String host, URL url) {
        if (Constants.ANYHOST_VALUE.equals(configuratorUrl.getHost()) || host.equals(configuratorUrl.getHost())) {
            String configApplication = configuratorUrl.getParameter(Constants.APPLICATION_KEY,
                    configuratorUrl.getUsername());
            String currentApplication = url.getParameter(Constants.APPLICATION_KEY, url.getUsername());
            if (configApplication == null || Constants.ANY_VALUE.equals(configApplication)
                    || configApplication.equals(currentApplication)) {
                Set<String> condtionKeys = new HashSet<String>();
                condtionKeys.add(Constants.CATEGORY_KEY);
                condtionKeys.add(Constants.CHECK_KEY);
                condtionKeys.add(Constants.DYNAMIC_KEY);
                condtionKeys.add(Constants.ENABLED_KEY);
                for (Map.Entry<String, String> entry : configuratorUrl.getParameters().entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key.startsWith("~") || Constants.APPLICATION_KEY.equals(key) || Constants.SIDE_KEY.equals(key)) {
                        condtionKeys.add(key);
                        if (value != null && !Constants.ANY_VALUE.equals(value)
                                && !value.equals(url.getParameter(key.startsWith("~") ? key.substring(1) : key))) {
                            return url;
                        }
                    }
                }
                return doConfigure(url, configuratorUrl.removeParameters(condtionKeys));
            }
        }
        return url;
    }

    /**
     * 根据priority、host依次排序
     * priority值越大，优先级越高；
     * priority相同，特定host优先级高于anyhost 0.0.0.0
     *
     * @param o
     * @return
     */
    public int compareTo(Configurator o) {
        if (o == null) {
            return -1;
        }

        int ipCompare = getUrl().getHost().compareTo(o.getUrl().getHost());
        if (ipCompare == 0) {//ip相同，根据priority排序
            int i = getUrl().getParameter(Constants.PRIORITY_KEY, 0),
                    j = o.getUrl().getParameter(Constants.PRIORITY_KEY, 0);
            if (i < j) {
                return -1;
            } else if (i > j) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return ipCompare;
        }


    }

    protected abstract URL doConfigure(URL currentUrl, URL configUrl);

}
