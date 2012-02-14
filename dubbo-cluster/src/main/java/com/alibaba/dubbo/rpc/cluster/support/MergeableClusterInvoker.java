/**
 * File Created at 2012-02-09
 * $Id$
 *
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.rpc.cluster.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.alibaba.dubbo.rpc.cluster.utils.ArrayMerger;
import com.alibaba.dubbo.rpc.cluster.utils.CollectionMerger;
import com.alibaba.dubbo.rpc.cluster.utils.MapMerger;
import com.alibaba.dubbo.rpc.cluster.utils.ResultMerger;

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

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class MergeableClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger log = LoggerFactory.getLogger( MergeableClusterInvoker.class );
    
    private ExecutorService executor = Executors.newCachedThreadPool(
            new NamedThreadFactory("mergeable-cluster-executor", true));

    public MergeableClusterInvoker( Directory<T> directory ) {
        super( directory );
    }

    @Override
    public Result invoke( final Invocation invocation ) throws RpcException {

        checkWheatherDestoried();

        int timeout = getUrl().getParameter( Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT );

        List<Invoker<T>> invokers = directory.list( invocation );

        Map<String, Future<Result>> results = new HashMap<String, Future<Result>>();

        for( final Invoker<T> invoker : invokers ) {

            Future<Result> future = executor.submit( new Callable<Result>() {

                public Result call() throws Exception {

                    return invoker.invoke( invocation );
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

                if ( returnType == void.class ) { continue; }

                if ( firstFlag ) {
                    firstFlag = false;
                    result = r.getResult();
                    timeout = 0;
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

                            if ( ExtensionLoader.getExtensionLoader( ResultMerger.class )
                                    .hasExtension( hint ) ) {
                                result = ExtensionLoader.getExtensionLoader( ResultMerger.class )
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

    @Override
    protected Result doInvoke( Invocation invocation, List list, LoadBalance loadbalance ) throws RpcException {
        throw new UnsupportedOperationException();
    }

}
