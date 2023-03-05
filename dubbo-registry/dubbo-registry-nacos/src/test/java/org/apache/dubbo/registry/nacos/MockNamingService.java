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
package org.apache.dubbo.registry.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.api.selector.AbstractSelector;

import java.util.List;

public class MockNamingService implements NamingService {
    @Override
    public void registerInstance(String serviceName, String ip, int port) {

    }

    @Override
    public void registerInstance(String serviceName, String groupName, String ip, int port) {

    }

    @Override
    public void registerInstance(String serviceName, String ip, int port, String clusterName) {

    }

    @Override
    public void registerInstance(String serviceName, String groupName, String ip, int port, String clusterName) {

    }

    @Override
    public void registerInstance(String serviceName, Instance instance) {

    }

    @Override
    public void registerInstance(String serviceName, String groupName, Instance instance) throws NacosException {

    }

    @Override
    public void batchRegisterInstance(String serviceName, String groupName, List<Instance> instances) {

    }

    @Override
    public void deregisterInstance(String serviceName, String ip, int port) {

    }

    @Override
    public void deregisterInstance(String serviceName, String groupName, String ip, int port) {

    }

    @Override
    public void deregisterInstance(String serviceName, String ip, int port, String clusterName) {

    }

    @Override
    public void deregisterInstance(String serviceName, String groupName, String ip, int port, String clusterName) {

    }

    @Override
    public void deregisterInstance(String serviceName, Instance instance) {

    }

    @Override
    public void deregisterInstance(String serviceName, String groupName, Instance instance) {

    }

    @Override
    public List<Instance> getAllInstances(String serviceName) {
        return null;
    }

    @Override
    public List<Instance> getAllInstances(String serviceName, String groupName) throws NacosException {
        return null;
    }

    @Override
    public List<Instance> getAllInstances(String serviceName, boolean subscribe) throws NacosException {
        return null;
    }

    @Override
    public List<Instance> getAllInstances(String serviceName, String groupName, boolean subscribe) {
        return null;
    }

    @Override
    public List<Instance> getAllInstances(String serviceName, List<String> clusters) {
        return null;
    }

    @Override
    public List<Instance> getAllInstances(String serviceName, String groupName, List<String> clusters) {
        return null;
    }

    @Override
    public List<Instance> getAllInstances(String serviceName, List<String> clusters, boolean subscribe) {
        return null;
    }

    @Override
    public List<Instance> getAllInstances(String serviceName, String groupName, List<String> clusters, boolean subscribe) {
        return null;
    }

    @Override
    public List<Instance> selectInstances(String serviceName, boolean healthy) {
        return null;
    }

    @Override
    public List<Instance> selectInstances(String serviceName, String groupName, boolean healthy) {
        return null;
    }

    @Override
    public List<Instance> selectInstances(String serviceName, boolean healthy, boolean subscribe) {
        return null;
    }

    @Override
    public List<Instance> selectInstances(String serviceName, String groupName, boolean healthy, boolean subscribe) {
        return null;
    }

    @Override
    public List<Instance> selectInstances(String serviceName, List<String> clusters, boolean healthy) {
        return null;
    }

    @Override
    public List<Instance> selectInstances(String serviceName, String groupName, List<String> clusters, boolean healthy) {
        return null;
    }

    @Override
    public List<Instance> selectInstances(String serviceName, List<String> clusters, boolean healthy, boolean subscribe) {
        return null;
    }

    @Override
    public List<Instance> selectInstances(String serviceName, String groupName, List<String> clusters, boolean healthy, boolean subscribe) {
        return null;
    }

    @Override
    public Instance selectOneHealthyInstance(String serviceName) {
        return null;
    }

    @Override
    public Instance selectOneHealthyInstance(String serviceName, String groupName) {
        return null;
    }

    @Override
    public Instance selectOneHealthyInstance(String serviceName, boolean subscribe) {
        return null;
    }

    @Override
    public Instance selectOneHealthyInstance(String serviceName, String groupName, boolean subscribe) {
        return null;
    }

    @Override
    public Instance selectOneHealthyInstance(String serviceName, List<String> clusters) {
        return null;
    }

    @Override
    public Instance selectOneHealthyInstance(String serviceName, String groupName, List<String> clusters) {
        return null;
    }

    @Override
    public Instance selectOneHealthyInstance(String serviceName, List<String> clusters, boolean subscribe) {
        return null;
    }

    @Override
    public Instance selectOneHealthyInstance(String serviceName, String groupName, List<String> clusters, boolean subscribe) {
        return null;
    }

    @Override
    public void subscribe(String serviceName, EventListener listener) throws NacosException {

    }

    @Override
    public void subscribe(String serviceName, String groupName, EventListener listener) throws NacosException {

    }

    @Override
    public void subscribe(String serviceName, List<String> clusters, EventListener listener) throws NacosException {

    }

    @Override
    public void subscribe(String serviceName, String groupName, List<String> clusters, EventListener listener) throws NacosException {

    }

    @Override
    public void unsubscribe(String serviceName, EventListener listener) {

    }

    @Override
    public void unsubscribe(String serviceName, String groupName, EventListener listener) {

    }

    @Override
    public void unsubscribe(String serviceName, List<String> clusters, EventListener listener) {

    }

    @Override
    public void unsubscribe(String serviceName, String groupName, List<String> clusters, EventListener listener) {

    }

    @Override
    public ListView<String> getServicesOfServer(int pageNo, int pageSize) {
        return null;
    }

    @Override
    public ListView<String> getServicesOfServer(int pageNo, int pageSize, String groupName) {
        return null;
    }

    @Override
    public ListView<String> getServicesOfServer(int pageNo, int pageSize, AbstractSelector selector) {
        return null;
    }

    @Override
    public ListView<String> getServicesOfServer(int pageNo, int pageSize, String groupName, AbstractSelector selector) {
        return null;
    }

    @Override
    public List<ServiceInfo> getSubscribeServices() {
        return null;
    }

    @Override
    public String getServerStatus() {
        return null;
    }

    @Override
    public void shutDown() {

    }
}
