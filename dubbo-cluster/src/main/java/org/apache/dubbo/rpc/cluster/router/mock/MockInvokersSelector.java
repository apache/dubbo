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
package org.apache.dubbo.rpc.cluster.router.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.router.state.RouterGroupingState;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.rpc.cluster.Constants.INVOCATION_NEED_MOCK;
import static org.apache.dubbo.rpc.cluster.Constants.MOCK_PROTOCOL;

/**
 * A specific Router designed to realize mock feature.
 * If a request is configured to use mock, then this router guarantees that only the invokers with protocol MOCK appear in final the invoker list, all other invokers will be excluded.
 */
public class MockInvokersSelector<T> extends AbstractStateRouter<T> {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MockInvokersSelector.class);
    
    public static final String NAME = "MOCK_ROUTER";

    private volatile BitList<Invoker<T>> normalInvokers = BitList.emptyList();
    private volatile BitList<Invoker<T>> mockedInvokers = BitList.emptyList();

    public MockInvokersSelector(URL url) {
        super(url);
    }

    @Override
    protected BitList<Invoker<T>> doRoute(
            BitList<Invoker<T>> invokers,
            URL url,
            Invocation invocation,
            boolean needToPrintMessage,
            Holder<RouterSnapshotNode<T>> nodeHolder,
            Holder<String> messageHolder)
            throws RpcException {
        
        logger.info("[MOCK-SELECTOR] doRoute called with {} invokers [Thread: {}]", invokers.size(), Thread.currentThread().getName());
        
        // 记录输入的invokers
        for (int i = 0; i < invokers.size(); i++) {
            Invoker<T> invoker = invokers.get(i);
            String clusterID = invoker.getUrl().getParameter("clusterID");
            String address = invoker.getUrl().getAddress();
            String protocol = invoker.getUrl().getProtocol();
            logger.info("[MOCK-SELECTOR] Input invoker[{}]: {} (clusterID: {}, protocol: {}) [Thread: {}]", i, address, clusterID, protocol, Thread.currentThread().getName());
        }
        
        // 记录normalInvokers缓存
        logger.info("[MOCK-SELECTOR] normalInvokers cache size: {} [Thread: {}]", normalInvokers.size(), Thread.currentThread().getName());
        for (int i = 0; i < normalInvokers.size(); i++) {
            Invoker<T> invoker = normalInvokers.get(i);
            String clusterID = invoker.getUrl().getParameter("clusterID");
            String address = invoker.getUrl().getAddress();
            String protocol = invoker.getUrl().getProtocol();
            logger.info("[MOCK-SELECTOR] Cached normal invoker[{}]: {} (clusterID: {}, protocol: {}) [Thread: {}]", i, address, clusterID, protocol, Thread.currentThread().getName());
        }
        
        if (CollectionUtils.isEmpty(invokers)) {
            if (needToPrintMessage) {
                messageHolder.set("Empty invokers. Directly return.");
            }
            return invokers;
        }

        BitList<Invoker<T>> result;
        if (invocation.getObjectAttachments() == null) {
            if (needToPrintMessage) {
                messageHolder.set("ObjectAttachments from invocation are null. Return normal Invokers.");
            }
            result = invokers.and(normalInvokers);
        } else {
            String value = (String) invocation.getObjectAttachmentWithoutConvert(INVOCATION_NEED_MOCK);
            if (value == null) {
                if (needToPrintMessage) {
                    messageHolder.set("invocation.need.mock not set. Return normal Invokers.");
                }
                result = invokers.and(normalInvokers);
            } else if (Boolean.TRUE.toString().equalsIgnoreCase(value)) {
                if (needToPrintMessage) {
                    messageHolder.set("invocation.need.mock is true. Return mocked Invokers.");
                }
                result = invokers.and(mockedInvokers);
            } else {
                if (needToPrintMessage) {
                    messageHolder.set("Directly Return. Reason: invocation.need.mock is set but not match true");
                }
                result = invokers;
            }
        }
        
        // 记录and操作的结果
        logger.info("[MOCK-SELECTOR] After and() operation, result size: {} [Thread: {}]", result.size(), Thread.currentThread().getName());
        for (int i = 0; i < result.size(); i++) {
            Invoker<T> invoker = result.get(i);
            String clusterID = invoker.getUrl().getParameter("clusterID");
            String address = invoker.getUrl().getAddress();
            String protocol = invoker.getUrl().getProtocol();
            logger.info("[MOCK-SELECTOR] Result invoker[{}]: {} (clusterID: {}, protocol: {}) [Thread: {}]", i, address, clusterID, protocol, Thread.currentThread().getName());
        }
        
        return result;
    }

    @Override
    public void notify(BitList<Invoker<T>> invokers) {
        logger.info("[MOCK-SELECTOR] notify called with {} invokers [Thread: {}]", invokers.size(), Thread.currentThread().getName());
        
        // 记录notify的输入invokers
        for (int i = 0; i < invokers.size(); i++) {
            Invoker<T> invoker = invokers.get(i);
            String clusterID = invoker.getUrl().getParameter("clusterID");
            String address = invoker.getUrl().getAddress();
            String protocol = invoker.getUrl().getProtocol();
            logger.info("[MOCK-SELECTOR] Notify invoker[{}]: {} (clusterID: {}, protocol: {}) [Thread: {}]", i, address, clusterID, protocol, Thread.currentThread().getName());
        }
        
        // 记录缓存更新前的状态
        logger.info("[MOCK-SELECTOR] Before cache update: normalInvokers={}, mockedInvokers={} [Thread: {}]", normalInvokers.size(), mockedInvokers.size(), Thread.currentThread().getName());
        for (int i = 0; i < normalInvokers.size(); i++) {
            Invoker<T> invoker = normalInvokers.get(i);
            String clusterID = invoker.getUrl().getParameter("clusterID");
            String address = invoker.getUrl().getAddress();
            logger.info("[MOCK-SELECTOR] Before update - normal invoker[{}]: {} (clusterID: {}) [Thread: {}]", i, address, clusterID, Thread.currentThread().getName());
        }
        
        cacheMockedInvokers(invokers);
        cacheNormalInvokers(invokers);
        
        // 记录缓存更新后的状态
        logger.info("[MOCK-SELECTOR] After cache update: normalInvokers={}, mockedInvokers={} [Thread: {}]", normalInvokers.size(), mockedInvokers.size(), Thread.currentThread().getName());
        for (int i = 0; i < normalInvokers.size(); i++) {
            Invoker<T> invoker = normalInvokers.get(i);
            String clusterID = invoker.getUrl().getParameter("clusterID");
            String address = invoker.getUrl().getAddress();
            logger.info("[MOCK-SELECTOR] After update - normal invoker[{}]: {} (clusterID: {}) [Thread: {}]", i, address, clusterID, Thread.currentThread().getName());
        }
    }

    private void cacheMockedInvokers(BitList<Invoker<T>> invokers) {
        BitList<Invoker<T>> clonedInvokers = invokers.clone();
        clonedInvokers.removeIf((invoker) -> !invoker.getUrl().getProtocol().equals(MOCK_PROTOCOL));
        mockedInvokers = clonedInvokers;
    }

    @SuppressWarnings("rawtypes")
    private void cacheNormalInvokers(BitList<Invoker<T>> invokers) {
        BitList<Invoker<T>> clonedInvokers = invokers.clone();
        clonedInvokers.removeIf((invoker) -> invoker.getUrl().getProtocol().equals(MOCK_PROTOCOL));
        normalInvokers = clonedInvokers;
    }

    @Override
    protected String doBuildSnapshot() {
        Map<String, BitList<Invoker<T>>> grouping = new HashMap<>();
        grouping.put("Mocked", mockedInvokers);
        grouping.put("Normal", normalInvokers);
        return new RouterGroupingState<>(
                        this.getClass().getSimpleName(), mockedInvokers.size() + normalInvokers.size(), grouping)
                .toString();
    }
}
