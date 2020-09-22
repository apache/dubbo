/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package org.apache.dubbo.demo.consumer;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationClusterComparator;

import java.util.List;
import java.util.stream.Collectors;

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
public class RandomMigrationAddressComparator implements MigrationClusterComparator {
    @Override
    public <T> boolean shouldMigrate(List<Invoker<T>> interfaceInvokers, List<Invoker<T>> serviceInvokers) {
        List<Invoker<T>>  availableServiceInvokers = serviceInvokers.stream().filter( s -> ((MigrationClusterInvoker)s).isAvailable()).collect(Collectors.toList());
        List<Invoker<T>>  availableInterfaceInvokers = interfaceInvokers.stream().filter( s -> ((MigrationClusterInvoker)s).isAvailable()).collect(Collectors.toList());


        if (availableServiceInvokers.isEmpty()) {
            return false;
        }

        return availableServiceInvokers.size() >= availableInterfaceInvokers.size();
    }
}