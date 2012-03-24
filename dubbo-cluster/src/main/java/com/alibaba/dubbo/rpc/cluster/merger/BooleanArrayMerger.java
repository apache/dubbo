/*
 * Copyright 1999-2012 Alibaba Group.
 *    
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *    
 *        http://www.apache.org/licenses/LICENSE-2.0
 *    
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class BooleanArrayMerger implements Merger<boolean[]> {

    public boolean[] merge(boolean[]... items) {
        int totalLen = 0;
        for(boolean[] array : items) {
            totalLen += array.length;
        }
        boolean[] result = new boolean[totalLen];
        int index = 0;
        for(boolean[] array : items) {
            for(boolean item : array) {
                result[index++] = item;
            }
        }
        return result;
    }

}
