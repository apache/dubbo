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

import java.lang.reflect.Array;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@Extension( ArrayMerger.NAME )
public class ArrayMerger implements ResultMerger {

    public static final String NAME = "array";

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
