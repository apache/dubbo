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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathExpression;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition.PathSegment.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * A high-performance Radix Tree for efficient path matching.
 *
 * @param <T> Type of values associated with the paths.
 */
public final class RadixTree<T> {

    private final Map<String, List<Match<T>>> directPathMap = new HashMap<>();
    private final Node<T> root = new Node<>();

    public T addPath(PathExpression path, T value) {
        if (path.isDirect()) {
            List<Match<T>> matches = directPathMap.computeIfAbsent(path.getPath(), k -> new ArrayList<>());
            for (int i = 0, len = matches.size(); i < len; i++) {
                Match<T> match = matches.get(i);
                if (match.getValue().equals(value)) {
                    return match.getValue();
                }
            }
            matches.add(new Match<>(path, value));
            return null;
        }

        Node<T> current = root;
        PathSegment[] segments = path.getSegments();
        for (int i = 0, len = segments.length; i < len; i++) {
            Node<T> child = getChild(current, segments[i]);
            if (i == len - 1) {
                List<Pair<PathExpression, T>> values = child.values;
                for (int j = 0, size = values.size(); j < size; j++) {
                    if (values.get(j).getLeft().equals(path)) {
                        return values.get(j).getRight();
                    }
                }
                values.add(Pair.of(path, value));
            }
            current = child;
        }
        return null;
    }

    private static <T> Node<T> getChild(Node<T> current, PathSegment segment) {
        Node<T> child;
        if (segment.getType() == Type.LITERAL) {
            Map<Key, Node<T>> children = current.children;
            Key key = new Key(segment.getValue());
            child = children.get(key);
            if (child == null) {
                child = new Node<>();
                children.put(key, child);
            }
        } else {
            Map<PathSegment, Node<T>> children = current.fuzzyChildren;
            child = children.get(segment);
            if (child == null) {
                child = new Node<>();
                children.put(segment, child);
            }
        }
        return child;
    }

    public void remove(Predicate<T> tester) {
        directPathMap.entrySet().removeIf(entry -> {
            List<Match<T>> values = entry.getValue();
            values.removeIf(match -> tester.test(match.getValue()));
            return values.isEmpty();
        });
        removeRecursive(root, tester);
    }

    private void removeRecursive(Node<T> current, Predicate<T> tester) {
        current.values.removeIf(pair -> tester.test(pair.getValue()));

        List<Map<?, Node<T>>> list = new ArrayList<>();
        list.add(current.children);
        list.add(current.fuzzyChildren);
        for (Map<?, Node<T>> children : list) {
            Iterator<? extends Entry<?, Node<T>>> cit = children.entrySet().iterator();
            while (cit.hasNext()) {
                Node<T> node = cit.next().getValue();
                removeRecursive(node, tester);
                if (node.isEmpty()) {
                    cit.remove();
                }
            }
        }
    }

    /**
     * Ensure that the path is normalized using {@link PathUtils#normalize(String)} before matching.
     */
    public void match(String path, List<Match<T>> matches) {
        List<Match<T>> directMatches = directPathMap.get(path);
        if (directMatches != null) {
            for (int i = 0, size = directMatches.size(); i < size; i++) {
                matches.add(directMatches.get(i));
            }
            return;
        }

        matchRecursive(root, path, 1, new HashMap<>(), matches);
    }

    public List<Match<T>> match(String path) {
        List<Match<T>> matches = directPathMap.get(path);
        if (matches != null) {
            return new ArrayList<>(matches);
        }

        matches = new ArrayList<>();
        matchRecursive(root, path, 1, new HashMap<>(), matches);
        return matches;
    }

    private void matchRecursive(
            Node<T> current, String path, int start, Map<String, String> variableMap, List<Match<T>> matches) {
        int end = path.indexOf('/', start);
        Node<T> node = current.children.get(new Key(path, start, end));
        if (node != null) {
            if (node.isLeaf()) {
                addMatch(node, variableMap, matches);
                return;
            }
            matchRecursive(node, path, end + 1, variableMap, matches);
        }

        if (current.fuzzyChildren.isEmpty()) {
            return;
        }
        Map<String, String> workVariableMap = new HashMap<>();
        for (Map.Entry<PathSegment, Node<T>> entry : current.fuzzyChildren.entrySet()) {
            PathSegment segment = entry.getKey();
            if (segment.match(path, start, end, workVariableMap)) {
                workVariableMap.putAll(variableMap);
                if (segment.isTailMatching()) {
                    addMatch(entry.getValue(), workVariableMap, matches);
                } else {
                    matchRecursive(entry.getValue(), path, end + 1, workVariableMap, matches);
                }
                if (!workVariableMap.isEmpty()) {
                    workVariableMap = new HashMap<>();
                }
            }
        }
    }

    private static <T> void addMatch(Node<T> node, Map<String, String> variableMap, List<Match<T>> matches) {
        List<Pair<PathExpression, T>> values = node.values;
        variableMap = variableMap.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(variableMap);
        for (int i = 0, size = values.size(); i < size; i++) {
            Pair<PathExpression, T> pair = values.get(i);
            matches.add(new Match<>(pair.getLeft(), pair.getRight(), variableMap));
        }
    }

    public void clear() {
        directPathMap.clear();
        root.clear();
    }

    public boolean isEmpty() {
        return directPathMap.isEmpty() && root.isEmpty();
    }

    public static final class Match<T> implements Comparable<Match<T>> {

        private final PathExpression expression;
        private final T value;
        private final Map<String, String> variableMap;

        Match(PathExpression expression, T value, Map<String, String> variableMap) {
            this.expression = expression;
            this.value = value;
            this.variableMap = variableMap;
        }

        private Match(PathExpression expression, T value) {
            this.expression = expression;
            this.value = value;
            variableMap = Collections.emptyMap();
        }

        public PathExpression getExpression() {
            return expression;
        }

        public T getValue() {
            return value;
        }

        public Map<String, String> getVariableMap() {
            return variableMap;
        }

        @Override
        public int compareTo(Match<T> other) {
            int comparison = expression.compareTo(other.getExpression());
            return comparison == 0 ? variableMap.size() - other.variableMap.size() : comparison;
        }
    }

    /**
     * Zero-copy string key.
     */
    private static final class Key implements CharSequence {

        private final String value;
        private final int offset;
        private final int length;

        private Key(String value, int start, int end) {
            this.value = value;
            offset = start;
            length = (end == -1 ? value.length() : end) - start;
        }

        public Key(String value) {
            this.value = value;
            offset = 0;
            length = value.length();
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public char charAt(int index) {
            return value.charAt(offset + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return value.substring(offset + start, offset + end);
        }

        @Override
        public int hashCode() {
            int h = 0;
            for (int i = 0; i < length; i++) {
                h = 31 * h + value.charAt(offset + i);
            }
            return h;
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            Key that = (Key) obj;
            return value.regionMatches(offset, that.value, that.offset, length);
        }

        @Override
        public String toString() {
            return value.substring(offset, length - offset);
        }
    }

    private static final class Node<T> {

        private final Map<Key, Node<T>> children = new HashMap<>();
        private final Map<PathSegment, Node<T>> fuzzyChildren = new HashMap<>();
        private final List<Pair<PathExpression, T>> values = new ArrayList<>();

        private boolean isLeaf() {
            return children.isEmpty() && fuzzyChildren.isEmpty();
        }

        private boolean isEmpty() {
            return isLeaf() && values.isEmpty();
        }

        private void clear() {
            children.clear();
            fuzzyChildren.clear();
            values.clear();
        }
    }
}
