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

package org.apache.dubbo.metrics.filter;

import java.util.function.Supplier;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;

import static org.apache.dubbo.common.constants.MetricsConstants.METRIC_FILTER_START_TIME;
import static org.apache.dubbo.rpc.support.RpcUtils.isGenericCall;

public class MetricsCollectExecutor {

    private final DefaultMetricsCollector collector;
    private final Invocation invocation;
    private final String applicationName;

    private String interfaceName;
    private String methodName;
    private String group;
    private String version;


    public MetricsCollectExecutor(String applicationName, DefaultMetricsCollector collector, Invocation invocation) {
        init(invocation);
        this.collector = collector;
        this.invocation = invocation;
        this.applicationName = applicationName;
    }

    public void beforeExecute() {
        collector.increaseTotalRequests(applicationName, interfaceName, methodName, group, version);
        collector.increaseProcessingRequests(applicationName, interfaceName, methodName, group, version);
        invocation.put(METRIC_FILTER_START_TIME, System.currentTimeMillis());
    }

    public void postExecute(Result result) {
        if (result.hasException()) {
            this.throwExecute(result.getException());
            return;
        }
        collector.increaseSucceedRequests(applicationName, interfaceName, methodName, group, version);
        endExecute();
    }

    public void throwExecute(Throwable throwable){
        if (throwable instanceof RpcException) {
            RpcException rpcException = (RpcException)throwable;
            switch (rpcException.getCode()) {

                case RpcException.TIMEOUT_EXCEPTION:
                    collector.timeoutRequests(applicationName, interfaceName, methodName, group, version);
                    break;

                case RpcException.LIMIT_EXCEEDED_EXCEPTION:
                    collector.limitRequests(applicationName, interfaceName, methodName, group, version);
                    break;

                case RpcException.BIZ_EXCEPTION:
                    collector.businessFailedRequests(applicationName, interfaceName, methodName, group, version);
                    break;

                default:
                    collector.increaseUnknownFailedRequests(applicationName, interfaceName, methodName, group, version);
            }
        }

        collector.totalFailedRequests(applicationName, interfaceName, methodName, group, version);

        endExecute(()-> throwable instanceof RpcException && ((RpcException) throwable).isBiz());
    }

    private void endExecute(){
        this.endExecute(() -> true);
    }

    private void endExecute(Supplier<Boolean> rtStat){
        if (rtStat.get()) {
            Long endTime = System.currentTimeMillis();
            Long beginTime = (Long) invocation.get(METRIC_FILTER_START_TIME);
            Long rt = endTime - beginTime;
            collector.addRT(applicationName, interfaceName, methodName, group, version, rt);
        }
        collector.decreaseProcessingRequests(applicationName, interfaceName, methodName, group, version);
    }

    private void init(Invocation invocation) {
        String serviceUniqueName = invocation.getTargetServiceUniqueName();
        String methodName = invocation.getMethodName();
        if (invocation instanceof RpcInvocation
            && isGenericCall(((RpcInvocation) invocation).getParameterTypesDesc(), methodName)
            && invocation.getArguments() != null
            && invocation.getArguments().length == 3) {
            methodName = ((String) invocation.getArguments()[0]).trim();
        }
        String group = null;
        String interfaceAndVersion;
        String[] arr = serviceUniqueName.split("/");
        if (arr.length == 2) {
            group = arr[0];
            interfaceAndVersion = arr[1];
        } else {
            interfaceAndVersion = arr[0];
        }

        String[] ivArr = interfaceAndVersion.split(":");
        String interfaceName = ivArr[0];
        String version = ivArr.length == 2 ? ivArr[1] : null;

        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.group = group;
        this.version = version;
    }
}
