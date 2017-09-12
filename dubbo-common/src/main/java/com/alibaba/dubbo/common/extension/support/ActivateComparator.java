package com.alibaba.dubbo.common.extension.support;

import com.alibaba.dubbo.common.extension.Activate;
import com.google.common.collect.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhenyu.nie created on 2016 2016/11/24 15:19
 */
public class ActivateComparator<T> {

    private Map<String, Item<T>> map = new HashMap<String, Item<T>>();

    public void add(String name, Activate activate, T object) {
        map.put(name, new Item<T>(name, activate, object));
    }

    public List<T> sort() {
        Map<String, Node<T>> nodes = Maps.newHashMapWithExpectedSize(map.size());
        final Multimap<Node<T>, Node<T>> edges = HashMultimap.create();
        for (Item<T> item : map.values()) {
            nodes.put(item.getName(), new Node<T>(item.getName(), item.getActivate().order(), item.getObject()));
        }
        for (Item<T> item : map.values()) {
            Activate activate = item.getActivate();
            String[] before = activate.before();
            putEdge(nodes, item, before, new PutEdge<T>() {
                @Override
                public void put(Node<T> lhs, Node<T> rhs) {
                    edges.put(lhs, rhs);
                }
            });

            String[] after = activate.after();
            putEdge(nodes, item, after, new PutEdge<T>() {
                @Override
                public void put(Node<T> lhs, Node<T> rhs) {
                    edges.put(rhs, lhs);
                }
            });
        }

        Sorter<T> sorter = new Sorter<T>(Sets.newHashSet(nodes.values()), edges);
        List<Node<T>> sortedNodes = sorter.sort();
        List<T> result = Lists.newArrayListWithCapacity(nodes.size());
        for (Node<T> node : sortedNodes) {
            result.add(node.getObject());
        }
        return result;
    }

    private void putEdge(Map<String, Node<T>> nodes, Item<T> item, String[] toPutNames, PutEdge<T> putEdge) {
        if (toPutNames.length > 0) {
            for (String putName : toPutNames) {
                Node<T> putNode = nodes.get(putName);
                if (putNode != null) {
                    putEdge.put(nodes.get(item.getName()), putNode);
                }
            }
        }
    }

    private interface PutEdge<T> {
        void put(Node<T> lhs, Node<T> rhs);
    }

    private static class Item<T> {
        private final String name;
        private final Activate activate;
        private final T object;

        public Item(String name, Activate activate, T object) {
            this.name = name;
            this.activate = activate;
            this.object = object;
        }

        public String getName() {
            return name;
        }

        public Activate getActivate() {
            return activate;
        }

        public T getObject() {
            return object;
        }
    }
}
