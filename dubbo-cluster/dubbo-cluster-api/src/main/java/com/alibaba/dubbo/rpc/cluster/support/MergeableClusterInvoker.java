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

import com.alibaba.dubbo.common.Constants;
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
import com.alibaba.dubbo.rpc.cluster.merger.MergerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@SuppressWarnings( "unchecked" )
public class MergeableClusterInvoker<T> implements Invoker<T> {

    private ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("mergeable-cluster-executor", true));
    
    private final Directory<T> directory;

    public MergeableClusterInvoker(Directory<T> directory) {
        this.directory = directory;
    }

    public Result invoke(final Invocation invocation) throws RpcException {
        int timeout = getUrl().getMethodParameter( invocation.getMethodName(), Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT );
        List<Invoker<T>> invokers = directory.list(invocation);
        
        Map<String, Future<Result>> results = new HashMap<String, Future<Result>>();
        for( final Invoker<T> invoker : invokers ) {
            Future<Result> future = executor.submit( new Callable<Result>() {
                public Result call() throws Exception {
                    return invoker.invoke(new RpcInvocation(invocation, invoker));
                }
            } );
            results.put( invoker.getUrl().getServiceKey(), future );
        }

        Object result = null;
        Class<?> returnType;
        try {
            returnType = getInterface().getMethod(
                    invocation.getMethodName(), invocation.getParameterTypes() ).getReturnType();
        } catch ( NoSuchMethodException e ) {
            throw new RpcException( e.getMessage(), e );
        }

        List<Result> resultList = new ArrayList<Result>( results.size() );
        
        for ( Map.Entry<String, Future<Result>> entry : results.entrySet() ) {
            Future<Result> future = entry.getValue();
            try {
                resultList.add( future.get( timeout, TimeUnit.MILLISECONDS ) );
            } catch ( Exception e ) {
                throw new RpcException( new StringBuilder( 32 )
                                                .append( "Failed to invoke service " )
                                                .append( entry.getKey() )
                                                .append( ": " )
                                                .append( e.getMessage() ).toString(),
                                        e );
            }
        }

        if ( returnType != void.class && resultList.size() > 0 ) {
            String merger = getUrl().getMethodParameter( invocation.getMethodName(), Constants.MERGER_KEY );
            if ( merger != null && !"".equals( merger.trim() ) ) {
                Method method;
                try {
                    method = returnType.getMethod( merger, returnType );
                } catch ( NoSuchMethodException e ) {
                    throw new RpcException( new StringBuilder( 32 )
                                                    .append( "Can not merge result because missing method [ " )
                                                    .append( merger )
                                                    .append( " ] in class [ " )
                                                    .append( returnType.getClass().getName() )
                                                    .append( " ]" )
                                                    .toString() );
                }
                if ( method != null ) {
                    if ( !Modifier.isPublic( method.getModifiers() ) ) {
                        method.setAccessible( true );
                    }
                    result = resultList.remove( 0 ).getValue();
                    try {
                        if ( method.getReturnType() != void.class
                                && method.getReturnType().isAssignableFrom( result.getClass() ) ) {
                            for ( Result r : resultList ) {
                                result = method.invoke( result, r.getValue() );
                            }
                        } else {
                            for ( Result r : resultList ) {
                                method.invoke( result, r.getValue() );
                            }
                        }
                    } catch ( Exception e ) {
                        throw new RpcException( 
                                new StringBuilder( 32 )
                                        .append( "Can not merge result: " )
                                        .append( e.getMessage() ).toString(), 
                                e );
                    }
                } else {
                    throw new RpcException(
                            new StringBuilder( 32 )
                                    .append( "Can not merge result because missing method [ " )
                                    .append( merger )
                                    .append( " ] in class [ " )
                                    .append( returnType.getClass().getName() )
                                    .append( " ]" )
                                    .toString() );
                }
            } else {
                Merger resultMerger = MergerFactory.getMerger(returnType);
                if (resultMerger != null) {
                    List args = new ArrayList(resultList.size());
                    for(Result r : resultList) {
                        args.add(r.getValue());
                    }
                    result = resultMerger.merge(
                            args.toArray((Object[])Array.newInstance(returnType, 0)));
                } else {
                    throw new RpcException( "There is no merger to merge result." );
                }
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
