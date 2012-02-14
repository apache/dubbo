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
package com.alibaba.dubbo.registry.support;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class CompositeDirectory<T> extends RegistryDirectory<T> {

    private ConcurrentHashMap<String, Invoker<T>> group2Invoker =
            new ConcurrentHashMap<String, Invoker<T>>();

    public CompositeDirectory( Class<T> serviceType, URL url ) {

        super( serviceType, url );
    }
    
    public void addInvoker( String group, Invoker<T> invoker ) {

        group2Invoker.putIfAbsent( group, invoker );
    }

    @Override
    public List<Invoker<T>> doList( Invocation invocation ) {
        return new ArrayList<Invoker<T>>( group2Invoker.values() );
    }

    public Map<String, Invoker<T>> getInvokers() {
        return Collections.unmodifiableMap( group2Invoker );
    }

    @Override
    public void destroy() {

        for( Invoker<T> invoker : group2Invoker.values() ) {
            invoker.destroy();
        }

        group2Invoker.clear();

    }

}
