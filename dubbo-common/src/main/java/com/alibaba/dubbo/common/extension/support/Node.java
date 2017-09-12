package com.alibaba.dubbo.common.extension.support;

import com.google.common.collect.ComparisonChain;

/**
 * @author zhenyu.nie created on 2016 2016/11/23 18:16
 */
class Node<T> implements Comparable<Node> {

    private final String name;

    private final int order;

    private final T object;

    public Node(Node<T> node) {
        this.name = node.getName();
        this.order = node.getOrder();
        this.object = node.getObject();
    }

    public Node(String name, int order, T object) {
        this.name = name;
        this.order = order;
        this.object = object;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public T getObject() {
        return object;
    }

    @Override
    public int compareTo(Node o) {
        return ComparisonChain.start()
                .compare(order, o.getOrder())
                .compare(name, o.getName())
                .result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        return name != null ? name.equals(node.name) : node.name == null;

    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Node{" +
                "name='" + name + '\'' +
                ", order=" + order +
                ", object=" + object +
                '}';
    }
}
