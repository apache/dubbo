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
public class MapMerger implements Merger<Map<?, ?>> {

    public Map<?, ?> merge(Map<?, ?>... items) {
        if (items.length == 0) {
            return null;
        }
        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Map<?, ?> item : items) {
            if (item != null) {
                result.putAll(item);
            }
        }
        return result;
    }

}
