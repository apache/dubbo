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
package org.apache.dubbo.remoting.etcd;

import org.apache.dubbo.common.URL;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface EtcdClient {

    /**
     * save the specified path to the etcd registry.
     *
     * @param path the path to be saved
     */
    void create(String path);

    /**
     * save the specified path to the etcd registry.
     * if node disconnect from etcd, it will be deleted
     * automatically by etcd when sessian timeout.
     *
     * @param path the path to be saved
     * @return the lease of current path.
     */
    long createEphemeral(String path);

    /**
     * remove the specified  from etcd registry.
     *
     * @param path the path to be removed
     */
    void delete(String path);

    /**
     * find direct children directory, excluding path self,
     * Never return null.
     *
     * @param path the path to be found direct children.
     * @return direct children directory, contains zero element
     * list if children directory not exists.
     */
    List<String> getChildren(String path);

    /**
     * register children listener for specified path.
     *
     * @param path     the path to be watched when children is added, delete or update.
     * @param listener when children is changed , listener will be trigged.
     * @return direct children directory, contains zero element
     * list if children directory not exists.
     */
    List<String> addChildListener(String path, ChildListener listener);

    /**
     * find watcher of the children listener for specified path.
     *
     * @param path     the path to be watched when children is added, delete or update.
     * @param listener when children is changed , listener will be trigged.
     * @return watcher if find else null
     */
    <T> T getChildListener(String path, ChildListener listener);

    /**
     * unregister children lister for specified path.
     *
     * @param path     the path to be unwatched .
     * @param listener when children is changed , lister will be trigged.
     */
    void removeChildListener(String path, ChildListener listener);

    /**
     * support connection notify if connection state was changed.
     *
     * @param listener if state changed, listener will be triggered.
     */
    void addStateListener(StateListener listener);

    /**
     * remove connection notify if connection state was changed.
     *
     * @param listener remove already registered listener, if listener
     *                 not exists nothing happened.
     */
    void removeStateListener(StateListener listener);

    /**
     * test if current client is active.
     *
     * @return true if connection is active else false.
     */
    boolean isConnected();

    /**
     * close current client and release all resourses.
     */
    void close();

    URL getUrl();

    /***
     * create new lease from specified second ,it should be waiting if failed.<p>
     *
     * @param second lease time (support second only).
     * @return lease id from etcd
     */
    long createLease(long second);

    /***
     * create new lease from specified ttl second before waiting specified timeout.<p>
     *
     * @param ttl lease time (support second only).
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException if this future completed exceptionally
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     * @return lease id from etcd
     */
    public long createLease(long ttl, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * revoke specified lease, any associated path will removed automatically.
     *
     * @param lease to be removed lease
     */
    void revokeLease(long lease);


    /**
     * Get the value of the specified key.
     * @param key the specified key
     * @return null if the value is not found
     */
    String getKVValue(String key);

    /**
     * Put the key value pair to etcd
     * @param key the specified key
     * @param value the paired value
     * @return true if put success
     */
    boolean put(String key, String value);

}
