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
package org.apache.dubbo.rpc.cluster.router.address;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AddressInvokersSelector extends AbstractRouter {

    public static final String NAME = "ADDRESS_ROUTER";

    private static final int ADDRESS_INVOKERS_DEFAULT_PRIORITY = 180;

    public AddressInvokersSelector() {
        this.priority = ADDRESS_INVOKERS_DEFAULT_PRIORITY;
    }

    @Override
    public <T> List<Invoker<T>> route(final List<Invoker<T>> invokers,
                                      URL url, final Invocation invocation) throws RpcException {
        if (CollectionUtils.isEmpty(invokers)) {
            return invokers;
        }

        if (invocation.getObjectAttachments() != null) {
            Address address = Address.class.cast(invocation.getObjectAttachment(AddressRouterFactory.NAME));
            if (Optional.ofNullable(address).isPresent()) {
                invocation.getObjectAttachments().remove(AddressRouterFactory.NAME);
                return invokers.stream().filter(it -> it.getUrl().getIp().equals(address.getIp()) && (it.getUrl().getPort() == address.getPort()) && it.isAvailable()).collect(Collectors.toList());
            }
        }
        return invokers;
    }


}
