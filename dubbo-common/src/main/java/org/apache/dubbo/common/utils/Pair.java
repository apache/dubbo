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
package org.apache.dubbo.common.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Pair<L, R> implements Map.Entry<L, R>, Comparable<Pair<L, R>>, Serializable {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("rawtypes")
    private static final Pair NULL = new Pair<>(null, null);

    private final L left;
    private final R right;

    public static <L, R> Pair<L, R> of(L left, R right) {
        return left == null && right == null ? nullPair() : new Pair<>(left, right);
    }

    @SuppressWarnings("unchecked")
    public static <L, R> Pair<L, R> nullPair() {
        return NULL;
    }

    @SafeVarargs
    public static <L, R> Map<L, R> toMap(Pair<L, R>... pairs) {
        if (pairs == null) {
            return Collections.emptyMap();
        }
        return toMap(Arrays.asList(pairs));
    }

    public static <L, R> Map<L, R> toMap(Collection<Pair<L, R>> pairs) {
        if (pairs == null) {
            return Collections.emptyMap();
        }
        Map<L, R> map = CollectionUtils.newLinkedHashMap(pairs.size());
        for (Pair<L, R> pair : pairs) {
            map.put(pair.getLeft(), pair.getRight());
        }
        return map;
    }

    public static <L, R> List<Pair<L, R>> toPairs(Map<L, R> map) {
        if (map == null) {
            return Collections.emptyList();
        }
        List<Pair<L, R>> pairs = new ArrayList<>(map.size());
        for (Map.Entry<L, R> entry : map.entrySet()) {
            pairs.add(of(entry.getKey(), entry.getValue()));
        }
        return pairs;
    }

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    public boolean isNull() {
        return this == NULL || left == null && right == null;
    }

    @Override
    public L getKey() {
        return left;
    }

    @Override
    public R getValue() {
        return right;
    }

    @Override
    public R setValue(R value) {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(Pair<L, R> other) {
        return left.equals(other.left)
                ? ((Comparable<R>) right).compareTo(other.right)
                : ((Comparable<L>) left).compareTo(other.left);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(left) ^ Objects.hashCode(right);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Map.Entry) {
            Map.Entry<?, ?> that = (Map.Entry<?, ?>) other;
            return Objects.equals(left, that.getKey()) && Objects.equals(right, that.getValue());
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + left + ", " + right + ')';
    }
}
