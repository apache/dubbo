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

import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class MapMerger implements Merger<Map<Object, Object>> {

    public static final String NAME = "map";

    public Map<Object, Object> merge(Map<Object, Object> r1, Map<Object, Object> r2) {

        if ( r1 == null ) {
            if ( r2 != null ) {
                return r2;
            } else {
                return null;
            }
        } else if ( r2 == null ) {
            return r1;
        }

        Map<Object, Object> result = null;
        
        if ( r1 instanceof Map && r2 instanceof Map ) {
            
            Map<Object, Object> map1 = ( Map<Object, Object> ) r1;
            Map<Object, Object> map2 = ( Map<Object, Object> ) r2;

            result = new HashMap<Object, Object>( map1.size() + map2.size() );

            result.putAll( map1 );
            result.putAll( map2 );

        }

        return result;
    }

}
