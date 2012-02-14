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

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
@Extension( MapMerger.NAME )
public class MapMerger implements ResultMerger {

    public static final String NAME = "map";

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

        Map result = null;
        
        if ( r1 instanceof Map && r2 instanceof Map ) {
            
            Map map1 = ( Map ) r1;
            Map map2 = ( Map ) r2;

            result = new HashMap( map1.size() + map2.size() );

            result.putAll( map1 );
            result.putAll( map2 );

        }

        return result;
    }

}
