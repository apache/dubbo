/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

/**
 * Short 数组 Merger 实现类
 */
public class ShortArrayMerger implements Merger<short[]> {

    @Override
    public short[] merge(short[]... items) {
        // 计算合并后的数组大小
        int total = 0;
        for (short[] array : items) {
            total += array.length;
        }
        // 创建结果数组
        short[] result = new short[total];
        // 合并多个数组
        int index = 0;
        for (short[] array : items) {
            for (short item : array) {
                result[index++] = item;
            }
        }
        return result;
    }

}