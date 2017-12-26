

package com.alibaba.dubbo.rpc.cluster.merger;

import com.alibaba.dubbo.rpc.cluster.Merger;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class SetMerger implements Merger<Set<?>> {

    public Set<Object> merge(Set<?>... items) {

        Set<Object> result = new HashSet<Object>();

        for (Set<?> item : items) {
            if (item != null) {
                result.addAll(item);
            }
        }

        return result;
    }
}
