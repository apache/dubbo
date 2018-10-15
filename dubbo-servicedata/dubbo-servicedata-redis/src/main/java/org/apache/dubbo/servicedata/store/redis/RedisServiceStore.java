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
package org.apache.dubbo.servicedata.store.redis;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.servicedata.support.AbstractServiceStore;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * ZookeeperRegistry
 */
public class RedisServiceStore extends AbstractServiceStore {

    private final static Logger logger = LoggerFactory.getLogger(RedisServiceStore.class);

    private final static String TAG = "sd.";

    final JedisPool pool;

    public RedisServiceStore(URL url) {
        super(url);
        pool = new JedisPool(new JedisPoolConfig(), url.getHost(), url.getPort());
    }

    @Override
    protected void doPutService(URL url) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(getKey(url), url.toParameterString());
        } catch (Throwable e) {
            logger.error("Failed to put " + url + " to redis " + url + ", cause: " + e.getMessage(), e);
            throw new RpcException("Failed to put " + url + " to redis " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    protected URL doPeekService(URL url) {
        try (Jedis jedis = pool.getResource()) {
            String value = jedis.get(getKey(url));
            if (value == null) {
                return null;
            }
            return url.addParameterString(value);
        } catch (Throwable e) {
            logger.error("Failed to peek " + url + " to redis " + url + ", cause: " + e.getMessage(), e);
            throw new RpcException("Failed to put " + url + " to redis " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    String getKey(URL url) {
        String protocol = getProtocol(url);
        String app = url.getParameter(Constants.APPLICATION_KEY);
        String appStr = Constants.PROVIDER_PROTOCOL.equals(protocol) ? "" : (app == null ? "" : (app + "."));
        return TAG + protocol + "." + appStr + url.getServiceKey();
    }

    String getProtocol(URL url) {
        String protocol = url.getParameter(Constants.SIDE_KEY);
        protocol = protocol == null ? url.getProtocol() : protocol;
        return protocol;
    }


}
