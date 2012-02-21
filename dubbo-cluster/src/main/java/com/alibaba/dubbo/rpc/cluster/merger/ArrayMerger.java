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

import com.alibaba.dubbo.rpc.cluster.Merger;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@SuppressWarnings( "unchecked" )
public class ArrayMerger implements Merger<Object> {

    public static final String NAME = "array";

    public static final ArrayMerger INSTANCE = new ArrayMerger();

    public Object merge(Object... others) {

        if ( others.length == 0 ) { return null; }

        List list = new ArrayList();
        
        for( int i = 0; i < others.length; i++ ) {
            Object item = others[i];
            if ( item != null ) {
                if ( item.getClass().isArray() ) {
                    int len = Array.getLength( item );
                    for( int j = 0; j < len; j++ ) {
                        Object obj = Array.get( item, j );
                        if ( obj != null ) {
                            list.add( obj );
                        }
                    }
                } else {
                    throw new IllegalArgumentException( 
                            new StringBuilder(32).append( i + 1 )
                                    .append( "th argument is not an array" ).toString() );
                }
            }
        }

        return list.toArray();

    }

}
