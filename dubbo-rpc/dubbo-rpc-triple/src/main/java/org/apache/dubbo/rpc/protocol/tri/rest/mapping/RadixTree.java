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
import org.apache.dubbo.rpc.protocol.tri.rest.util.KeyString;
import org.apache.dubbo.rpc.protocol.tri.rest.util.PathUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * A high-performance Radix Tree for efficient path matching.
 *
 * @param <T> Type of values associated with the paths.
 */
public final class RadixTree<T> {

    private final Map<KeyString, List<Match<T>>> directPathMap = new HashMap<>();
    private final Node<T> root = new Node<>();
    private final char separator;
    private final boolean caseSensitive;

    public RadixTree(boolean caseSensitive, char separator) {
        this.caseSensitive = caseSensitive;
        this.separator = separator;
    }

    public RadixTree(boolean caseSensitive) {
        this(caseSensitive, '/');
    }

    public RadixTree(char separator) {
        this(true, separator);
    }

    public RadixTree() {
        this(true, '/');
    }

    public T addPath(PathExpression path, T value) {
        if (path.isDirect()) {
            KeyString key = new KeyString(path.getPath(), caseSensitive);
            List<Match<T>> matches = directPathMap.computeIfAbsent(key, k -> new ArrayList<>());
            for (int i = 0, size = matches.size(); i < size; i++) {
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

    public T addPath(String path, T value) {
        if (path == null) {
            return value;
        }
        if (separator == '/') {
            path = PathUtils.normalize(path);
        } else {
            path = path.replace(separator, '/');
            if (path.isEmpty() || path.charAt(0) != '/') {
                path = '/' + path;
            }
        }
        return addPath(PathExpression.parse(path), value);
    }

    public void addPath(T value, String... paths) {
        for (String path : paths) {
            addPath(path, value);
        }
    }

    private Node<T> getChild(Node<T> current, PathSegment segment) {
        Node<T> child;
        if (segment.getType() == Type.LITERAL) {
            Map<KeyString, Node<T>> children = current.children;
            KeyString key = new KeyString(segment.getValue(), caseSensitive);
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

    public void walk(BiConsumer<PathExpression, T> consumer) {
        for (List<Match<T>> matches : directPathMap.values()) {
            for (Match<T> match : matches) {
                consumer.accept(match.getExpression(), match.getValue());
            }
        }
        walkRecursive(root, consumer);
    }

    private void walkRecursive(Node<T> root, BiConsumer<PathExpression, T> consumer) {
        for (Pair<PathExpression, T> pair : root.values) {
            consumer.accept(pair.getLeft(), pair.getRight());
        }

        for (Node<T> node : root.children.values()) {
            walkRecursive(node, consumer);
        }

        for (Node<T> node : root.fuzzyChildren.values()) {
            walkRecursive(node, consumer);
        }
    }

    /**
     * Ensure that the path is normalized using {@link PathUtils#normalize(String)} before matching.
     */
    public void match(KeyString path, List<Match<T>> matches) {
        List<Match<T>> directMatches = directPathMap.get(path);
        if (directMatches != null) {
            for (int i = 0, size = directMatches.size(); i < size; i++) {
                matches.add(directMatches.get(i));
            }
        }

        if (root.isLeaf()) {
            return;
        }
        matchRecursive(root, path, 1, new HashMap<>(), matches);
    }

    public void match(String path, List<Match<T>> matches) {
        match(new KeyString(path, caseSensitive), matches);
    }

    public List<Match<T>> match(KeyString path) {
        List<Match<T>> matches = directPathMap.get(path);
        if (matches == null) {
            if (root.isLeaf()) {
                return Collections.emptyList();
            }
            matches = new ArrayList<>();
        } else {
            if (root.isLeaf()) {
                return Collections.unmodifiableList(matches);
            }
            matches = new ArrayList<>(matches);
        }

        matchRecursive(root, path, 1, new HashMap<>(), matches);
        return matches;
    }

    public List<Match<T>> match(String path) {
        return match(new KeyString(path, caseSensitive));
    }

    public List<Match<T>> matchRelaxed(String path) {
        KeyString keyPath = new KeyString(path, caseSensitive);
        List<Match<T>> matches = new ArrayList<>();
        match(keyPath, matches);
        if (!matches.isEmpty()) {
            return matches;
        }

        int end = path.length();
        if (end > 1 && path.charAt(end - 1) == '/') {
            match(keyPath.subSequence(0, --end), matches);
            if (!matches.isEmpty()) {
                return matches;
            }
        }

        for (int i = end - 1; i >= 0; i--) {
            char ch = path.charAt(i);
            if (ch == '/') {
                break;
            }
            if (ch == '.') {
                match(keyPath.subSequence(0, i), matches);
                if (!matches.isEmpty()) {
                    return matches;
                }
            }
        }

        return matches;
    }

    private void matchRecursive(
            Node<T> current, KeyString path, int start, Map<String, String> variableMap, List<Match<T>> matches) {
        int end = -2;
        if (!current.children.isEmpty()) {
            end = path.indexOf(separator, start);
            Node<T> child = current.children.get(path.subSequence(start, end));
            if (child != null) {
                if (end == -1) {
                    addMatch(child, variableMap, matches);
                } else {
                    matchRecursive(child, path, end + 1, variableMap, matches);
                }
            }
        }

        if (current.fuzzyChildren.isEmpty()) {
            return;
        }
        if (end == -2) {
            end = path.indexOf(separator, start);
        }
        Map<String, String> workVariableMap = new LinkedHashMap<>();
        for (Map.Entry<PathSegment, Node<T>> entry : current.fuzzyChildren.entrySet()) {
            PathSegment segment = entry.getKey();
            if (segment.match(path, start, end, workVariableMap)) {
                workVariableMap.putAll(variableMap);
                Node<T> child = entry.getValue();
                if (segment.isTailMatching()) {
                    addMatch(child, workVariableMap, matches);
                } else {
                    if (end == -1) {
                        addMatch(child, workVariableMap, matches);
                    } else {
                        matchRecursive(child, path, end + 1, workVariableMap, matches);
                    }
                }
                if (!workVariableMap.isEmpty()) {
                    workVariableMap = new LinkedHashMap<>();
                }
            }
        }
    }

    private static <T> void addMatch(Node<T> node, Map<String, String> variableMap, List<Match<T>> matches) {
        List<Pair<PathExpression, T>> values = node.values;
        if (values.isEmpty()) {
            if (node.fuzzyChildren.isEmpty()) {
                return;
            }
            for (Entry<PathSegment, Node<T>> entry : node.fuzzyChildren.entrySet()) {
                if (entry.getKey().getType() == Type.WILDCARD_TAIL) {
                    addMatch(entry.getValue(), variableMap, matches);
                }
            }
            return;
        }
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

    private static final class Node<T> {

        private final Map<KeyString, Node<T>> children = new HashMap<>();
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
