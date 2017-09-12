package com.alibaba.dubbo.common.extension.support;

import com.google.common.collect.*;

import java.util.*;

/**
 * @author zhenyu.nie created on 2016 2016/11/25 19:16
 */
class Sorter<T> {

    private static final int DEFAULT_ORDER = 0;

    private Set<Node<T>> nodes;

    private Multimap<Node<T>, Node<T>> edges;

    private Multimap<Node<T>, Node<T>> reversedEdges;

    private Map<Node<T>, NodeWithPreNodes<T>> mappings = Maps.newHashMap();

    private LinkedHashSet<Node<T>> result = new LinkedHashSet<Node<T>>();

    public Sorter(Set<Node<T>> nodes, Multimap<Node<T>, Node<T>> edges) {
        this.nodes = nodes;
        this.edges = edges;

        this.reversedEdges = HashMultimap.create();
        for (Map.Entry<Node<T>, Node<T>> edge : edges.entries()) {
            reversedEdges.put(edge.getValue(), edge.getKey());
        }
        for (Node<T> node : nodes) {
            Set<Node<T>> preNodes = searchPreNodes(node);
            this.mappings.put(node, new NodeWithPreNodes<T>(node, preNodes));
        }

        checkCircle();
    }

    public List<Node<T>> sort() {
        sort(Sets.newHashSet(nodes));
        return Lists.newArrayList(result);
    }

    private void checkCircle() {
        Map<Node<T>, State> states = initStates();
        for (Node<T> node : nodes) {
            if (states.get(node) == State.unvisited) {
                checkCircle(node, states);
            }
        }
    }

    private void checkCircle(Node<T> node, Map<Node<T>, State> states) {
        states.put(node, State.visiting);
        for (Node<T> otherNode : edges.get(node)) {
            State state = states.get(otherNode);
            if (state == State.unvisited) {
                checkCircle(otherNode, states);
            } else if (state == State.visiting) {
                throw new HasCircleException(node.getName(), otherNode.getName());
            }
        }
        states.put(node, State.visited);
    }

    private void sort(Set<Node<T>> nodes) {
        if (nodes.isEmpty()) {
            return;
        }

        if (nodes.size() == 1) {
            result.add(nodes.iterator().next());
            return;
        }

        Node<T> node;
        // 先处理掉所有order小于default order的点，认为它们应该排在没有边的default order的点的前面
        // 0 -> -5 -> -7, choose -5，这个地方选择-5或者-7先做遍历都是有道理的，感觉不存在谁好谁坏，这里选择-5这种方式
        // 0 -> -5 -> -3, choose -5
        while ((node = findMinOrderLessDefaultNodeWithoutToAddLessDefaultPreNode(nodes)) != null) {
            Set<Node<T>> toAdd = getUnAddedNode(mappings.get(node).getPreNodes());
            sort(Sets.newHashSet(toAdd));
            nodes.remove(node);
            nodes.removeAll(toAdd);
            result.add(node);
        }

        while ((node = findMinWithoutUnAddPreNode(nodes)) != null) {
            nodes.remove(node);
            result.add(node);
        }
    }

    private Node<T> findMinWithoutUnAddPreNode(Set<Node<T>> nodes) {
        PriorityQueue<Node<T>> priorityQueue = new PriorityQueue<Node<T>>();
        for (Node<T> node : nodes) {
            if (!hasUnAddPreNode(node)) {
                priorityQueue.add(node);
            }
        }
        return priorityQueue.peek();
    }

    private boolean hasUnAddPreNode(Node<T> node) {
        return !Sets.difference(mappings.get(node).getPreNodes(), result).isEmpty();
    }

    private Set<Node<T>> getUnAddedNode(Set<Node<T>> nodes) {
        return Sets.newHashSet(Sets.difference(nodes, result));
    }

    private Node<T> findMinOrderLessDefaultNodeWithoutToAddLessDefaultPreNode(Set<Node<T>> nodes) {
        PriorityQueue<Node<T>> priorityQueue = new PriorityQueue<Node<T>>();
        for (Node<T> node : nodes) {
            if (node.getOrder() < DEFAULT_ORDER && !hasToAddLessDefaultPreNode(node)) {
                priorityQueue.add(node);
            }
        }
        return priorityQueue.peek();
    }

    private boolean hasToAddLessDefaultPreNode(Node<T> node) {
        Set<Node<T>> preNodes = mappings.get(node).getPreNodes();
        for (Node<T> preNode : preNodes) {
            if (preNode.getOrder() < DEFAULT_ORDER && !result.contains(preNode)) {
                return true;
            }
        }
        return false;
    }

    private Set<Node<T>> searchPreNodes(Node<T> node) {
        Map<Node<T>, State> states = initStates();
        Set<Node<T>> preNodes = Sets.newHashSet();
        searchPreNodes(node, preNodes, states);
        preNodes.remove(node);
        return preNodes;
    }

    private Map<Node<T>, State> initStates() {
        Map<Node<T>, State> states = Maps.newHashMapWithExpectedSize(nodes.size());
        for (Node<T> node : nodes) {
            states.put(node, State.unvisited);
        }
        return states;
    }

    private void searchPreNodes(Node<T> node, Set<Node<T>> preNodes, Map<Node<T>, State> states) {
        states.put(node, State.visiting);
        preNodes.add(node);
        for (Node<T> otherNode : reversedEdges.get(node)) {
            State state = states.get(otherNode);
            if (state == State.unvisited) {
                searchPreNodes(otherNode, preNodes, states);
            }
        }
        states.put(node, State.visited);
    }

    private static class NodeWithPreNodes<T> extends Node<T> {

        private final Set<Node<T>> preNodes;

        public NodeWithPreNodes(Node<T> node, Collection<Node<T>> preNodes) {
            super(node);
            this.preNodes = ImmutableSet.copyOf(preNodes);
        }

        public Set<Node<T>> getPreNodes() {
            return preNodes;
        }
    }

    private enum State {
        unvisited, visiting, visited
    }
}
