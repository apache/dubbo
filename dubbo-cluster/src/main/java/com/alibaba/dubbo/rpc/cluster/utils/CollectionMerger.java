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
package com.alibaba.dubbo.rpc.cluster.utils;

import com.alibaba.dubbo.common.Extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@Extension( CollectionMerger.NAME )
public class CollectionMerger implements ResultMerger {

    public static final String NAME = "collection";
    
    @SuppressWarnings( "unchecked" )
    public Object merge( Object r1, Object r2 ) {

        if ( r1 == null ) {
            if ( r2 != null ) {
                return r2;
            } else {
                return null;
            }
        } else if ( r2 == null ) {
            return r1;
        }

        Collection result = null;
        
        if ( r1 instanceof List && r2 instanceof List ) {
            
            List list1 = ( List ) r1;
            List list2 = ( List ) r2;
            
            result = new ArrayList( list1.size() + list2.size() );

            addAll( result, list1, list2 );

        } else if ( r1 instanceof Set && r2 instanceof Set ) {
            
            Set set1 = ( Set ) r1;
            Set set2 = ( Set ) r2;

            result = new HashSet( set1.size() + set2.size() );

            addAll( result, set1, set2 );

        } 
        
        return result;
        
    }

    @SuppressWarnings( "unchecked" )
    private static void addAll( Collection result, Collection c1, Collection c2 ) {

        for ( Iterator iterator = c1.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            if ( !result.contains( obj ) ) {
                result.add( obj );
            }
        }

        for ( Iterator iterator = c2.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            if ( !result.contains( obj ) ) {
                result.add( obj );
            }
        }

    }
    
}
