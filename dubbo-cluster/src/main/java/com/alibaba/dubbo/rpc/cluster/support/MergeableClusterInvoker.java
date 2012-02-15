/*
 * Copyright 1999-2011 Alibaba Group.
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
package com.alibaba.dubbo.rpc.cluster.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.Merger;
import com.alibaba.dubbo.rpc.cluster.merger.ArrayMerger;
import com.alibaba.dubbo.rpc.cluster.merger.CollectionMerger;
import com.alibaba.dubbo.rpc.cluster.merger.MapMerger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class MergeableClusterInvoker<T> implements Invoker<T> {

    private ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("mergeable-cluster-executor", true));
    
    private final Directory<T> directory;

    public MergeableClusterInvoker(Directory<T> directory) {
        this.directory = directory;
    }

    public Result invoke(final Invocation invocation) throws RpcException {
        int timeout = getUrl().getParameter( Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT );
        List<Invoker<T>> invokers = directory.list(invocation);
        
        Map<String, Future<Result>> results = new HashMap<String, Future<Result>>();
        for( final Invoker<T> invoker : invokers ) {
            Future<Result> future = executor.submit( new Callable<Result>() {
                public Result call() throws Exception {
                    return invoker.invoke(new RpcInvocation(invocation, invoker.getUrl()));
                }
            } );
            results.put( invoker.getUrl().getServiceKey(), future );
        }

        boolean firstFlag = true;
        Object result = null;
        Class<?> returnType;
        try {
            returnType = getInterface().getMethod(
                    invocation.getMethodName(), invocation.getParameterTypes() ).getReturnType();
        } catch ( NoSuchMethodException e ) {
            throw new RpcException( e.getMessage(), e );
        }

        for ( Map.Entry<String, Future<Result>> entry : results.entrySet() ) {
            Future<Result> future = entry.getValue();
            try {
                Result r = future.get( timeout, TimeUnit.MILLISECONDS );
                if (returnType == void.class) {
                    continue;
                }
                if ( firstFlag ) {
                    firstFlag = false;
                    result = r.getResult();
                } else {
                    if ( result == null ) {
                        result = r.getResult();
                    } else {
                        String merger = getUrl().getParameter( Constants.MERGER_KEY );
                        if ( merger != null && ! "".equals( merger.trim() ) ) {
                            Method method = returnType.getMethod( merger, returnType );
                            if ( method != null ) {
                                if ( !Modifier.isPublic( method.getModifiers() ) ) {
                                    method.setAccessible( true );
                                }
                                if ( method.getReturnType() != void.class
                                        && method.getReturnType().isAssignableFrom( result.getClass() ) ) {
                                    result = method.invoke( result, r.getResult() );
                                } else {
                                    method.invoke( result, r.getResult() );
                                }
                            } else {
                                throw new RpcException( new StringBuilder( 32 )
                                                                .append( "Can not merge result because missing method [ " )
                                                                .append( merger )
                                                                .append( " ] in class [ " )
                                                                .append( result.getClass().getName() )
                                                                .append( " ]" )
                                                                .toString() );
                            }
                        } else {
                            String hint;
                            if ( returnType.isArray() ) {
                                hint = ArrayMerger.NAME;
                            } else if ( Collection.class.isAssignableFrom( returnType ) ) {
                                hint = CollectionMerger.NAME;
                            } else if ( Map.class.isAssignableFrom( returnType ) ) {
                                hint = MapMerger.NAME;
                            } else {
                                throw new RpcException( "There is no merger to merge multi result" );
                            }
                            if ( ExtensionLoader.getExtensionLoader( Merger.class )
                                    .hasExtension( hint ) ) {
                                result = ExtensionLoader.getExtensionLoader( Merger.class )
                                        .getExtension( hint ).merge( result, r.getResult() );
                            } else {
                                throw new RpcException(
                                        "Could not merge multi result because of missing merger" );
                            }
                        }
                    }
                }
            } catch ( Exception e ) {
                throw new RpcException( new StringBuilder( 32 )
                                                .append( "Failed to invoke service " )
                                                .append( entry.getKey() )
                                                .append( ": " )
                                                .append( e.getMessage() ).toString(),
                                        e );
            }

        }

        return new RpcResult( result );
    }

    public Class<T> getInterface() {
        return directory.getInterface();
    }

    public URL getUrl() {
        return directory.getUrl();
    }

    public boolean isAvailable() {
        return directory.isAvailable();
    }

    public void destroy() {
        directory.destroy();
    }

}
