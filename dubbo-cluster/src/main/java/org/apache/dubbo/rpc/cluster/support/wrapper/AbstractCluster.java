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
package org.apache.dubbo.rpc.cluster.support.wrapper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.ClusterInterceptor;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Directory;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.REFERENCE_INTERCEPTOR_KEY;

public abstract class AbstractCluster implements Cluster {

    private <T> Invoker<T> buildClusterInterceptors(Invoker<T> invoker, String key) {
        Invoker<T> last = invoker;
        List<ClusterInterceptor> interceptors = ExtensionLoader.getExtensionLoader(ClusterInterceptor.class).getActivateExtension(invoker.getUrl(), key);

        if (!interceptors.isEmpty()) {
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                final ClusterInterceptor interceptor = interceptors.get(i);
                final Invoker<T> next = last;
                last = new Invoker<T>() {

                    @Override
                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    @Override
                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    @Override
                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }

                    @Override
                    public Result invoke(Invocation invocation) throws RpcException {
                        Result asyncResult;
                        try {
                            interceptor.before(next, invocation);
                            asyncResult = interceptor.invoke(next, invocation);
                        } catch (Exception e) {
                            // onError callback
                            if (interceptor instanceof ClusterInterceptor.Listener) {
                                ClusterInterceptor.Listener listener = (ClusterInterceptor.Listener) interceptor;
                                listener.onError(e, invoker, invocation);
                            }
                            throw e;
                        } finally {
                            interceptor.after(next, invocation);
                        }
                        return asyncResult.whenCompleteWithContext((r, t) -> {
                            // onResponse callback
                            if (interceptor instanceof ClusterInterceptor.Listener) {
                                ClusterInterceptor.Listener listener = (ClusterInterceptor.Listener) interceptor;
                                if (t == null) {
                                    listener.onResponse(r, invoker, invocation);
                                } else {
                                    listener.onError(t, invoker, invocation);
                                }
                            }
                        });
                    }

                    @Override
                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return last;
    }

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return buildClusterInterceptors(doJoin(directory), directory.getUrl().getParameter(REFERENCE_INTERCEPTOR_KEY));
    }

    protected abstract <T> Invoker<T> doJoin(Directory<T> directory) throws RpcException;
}
