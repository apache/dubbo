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

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.rpc.cluster.Merger;

import java.lang.reflect.Array;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@Extension( ArrayMerger.NAME )
public class ArrayMerger implements Merger<Object> {

    public static final String NAME = "array";

    public Object merge(Object r1, Object r2) {

        if ( r1 == null ) {
            if ( r2 != null ) {
                return r2;
            } else {
                return null;
            }
        } else if ( r2 == null ) {
            return r1;
        }

        Object result = null;

        if ( r1.getClass().isArray() && r2.getClass().isArray() ) {

            int len1 = Array.getLength( r1 );
            int len2 = Array.getLength( r2 );
            
            if ( len1 == 0 ) {
                if ( len2 == 0 ) {
                    return r1;
                } else {
                    return r2;
                }
            } else if ( len2 == 0 ) {
                return r1;
            }
            
            int dim1 = getDimensions( r1 );
            int dim2 = getDimensions( r2 );

            if ( dim1 != dim2 ) {
                throw new UnsupportedOperationException(
                        new StringBuilder( 32 )
                                .append( "Can not merge different dimensions of arrays: r1 " )
                                .append( dim1 ).append( " and r2 " ).append( dim2 ).toString() );
            }

            Object item = Array.get( r1, 0 );

            result = Array.newInstance( item.getClass(), len1 + len2 );
            System.arraycopy( r1, 0, result, 0, len1 );
            System.arraycopy( r2, 0, result, len1, len2 );

        }
        
        return result;

    }
    
    static int getDimensions( Object array ) {

        int result = 0;

        if ( array.getClass().isArray() ) {
            result++;
            int len = Array.getLength( array );
            if ( len > 0 ) {
                Object item = Array.get( array, 0 );
                if ( item.getClass().isArray() ) {
                    result += getDimensions( item );
                }
            }
        }

        return result;
        
    }

}
