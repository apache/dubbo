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
package com.alibaba.dubbo.rpc.cluster.merger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class CollectionMerger implements Merger<Collection<Object>> {

    public static final String NAME = "collection";
    
    public Collection<Object> merge(Collection<Object> r1, Collection<Object> r2) {

        if ( r1 == null ) {
            if ( r2 != null ) {
                return r2;
            } else {
                return null;
            }
        } else if ( r2 == null ) {
            return r1;
        }

        Collection<Object> result = null;
        
        if ( r1 instanceof List && r2 instanceof List ) {
            
            List<Object> list1 = ( List<Object> ) r1;
            List<Object> list2 = ( List<Object> ) r2;
            
            result = new ArrayList<Object>( list1.size() + list2.size() );

            addAll( result, list1, list2 );

        } else if ( r1 instanceof Set && r2 instanceof Set ) {
            
            Set<Object> set1 = ( Set<Object> ) r1;
            Set<Object> set2 = ( Set<Object> ) r2;

            result = new HashSet<Object>( set1.size() + set2.size() );

            addAll( result, set1, set2 );

        } 
        
        return result;
        
    }

    private static void addAll( Collection<Object> result, Collection<Object> c1, Collection<Object> c2 ) {

        for ( Iterator<Object> iterator = c1.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            if ( !result.contains( obj ) ) {
                result.add( obj );
            }
        }

        for ( Iterator<Object> iterator = c2.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            if ( !result.contains( obj ) ) {
                result.add( obj );
            }
        }

    }
    
}
