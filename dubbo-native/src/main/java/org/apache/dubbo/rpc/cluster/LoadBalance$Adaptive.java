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
package org.apache.dubbo.rpc.cluster;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
public class LoadBalance$Adaptive implements org.apache.dubbo.rpc.cluster.LoadBalance {
public org.apache.dubbo.rpc.Invoker select(java.util.List arg0, org.apache.dubbo.common.URL arg1, org.apache.dubbo.rpc.Invocation arg2) throws org.apache.dubbo.rpc.RpcException {
if (arg1 == null) throw new IllegalArgumentException("url == null");
org.apache.dubbo.common.URL url = arg1;
if (arg2 == null) throw new IllegalArgumentException("invocation == null"); String methodName = arg2.getMethodName();
String extName = url.getMethodParameter(methodName, "loadbalance", "random");
if(extName == null) throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.cluster.LoadBalance) name from url (" + url.toString() + ") use keys([loadbalance])");
ScopeModel scopeModel = ScopeModelUtil.getOrDefault(url.getScopeModel(), org.apache.dubbo.rpc.cluster.LoadBalance.class);
org.apache.dubbo.rpc.cluster.LoadBalance extension = (org.apache.dubbo.rpc.cluster.LoadBalance)scopeModel.getExtensionLoader(org.apache.dubbo.rpc.cluster.LoadBalance.class).getExtension(extName);
return extension.select(arg0, arg1, arg2);
}
}
