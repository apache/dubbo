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
package org.apache.dubbo.qos.textui;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.currentTimeMillis;
import static org.apache.dubbo.common.utils.StringUtils.EMPTY;
import static org.apache.dubbo.common.utils.StringUtils.length;
import static org.apache.dubbo.common.utils.StringUtils.repeat;

/**
 * tree
 */
public class TTree implements TComponent {

    private static final String STEP_FIRST_CHAR = "`---";
    private static final String STEP_NORMAL_CHAR = "+---";
    private static final String STEP_HAS_BOARD = "|   ";
    private static final String STEP_EMPTY_BOARD = "    ";

    // should output cost or not
    private final boolean isPrintCost;

    // tree node
    private final Node root;

    // current node
    private Node current;


    public TTree(boolean isPrintCost, String title) {
        this.root = new Node(title).markBegin().markEnd();
        this.current = root;
        this.isPrintCost = isPrintCost;
    }


    @Override
    public String rendering() {

        final StringBuilder treeSB = new StringBuilder();
        recursive(0, true, "", root, new Callback() {

            @Override
            public void callback(int deep, boolean isLast, String prefix, Node node) {

                final boolean hasChild = !node.children.isEmpty();
                final String stepString = isLast ? STEP_FIRST_CHAR : STEP_NORMAL_CHAR;
                final int stepStringLength = length(stepString);
                treeSB.append(prefix).append(stepString);

                int costPrefixLength = 0;
                if (hasChild) {
                    treeSB.append("+");
                }
                if (isPrintCost
                        && !node.isRoot()) {
                    final String costPrefix = String.format("[%s,%sms]", (node.endTimestamp - root.beginTimestamp), (node.endTimestamp - node.beginTimestamp));
                    costPrefixLength = length(costPrefix);
                    treeSB.append(costPrefix);
                }

                final Scanner scanner = new Scanner(new StringReader(node.data.toString()));
                try {
                    boolean isFirst = true;
                    while (scanner.hasNextLine()) {
                        if (isFirst) {
                            treeSB.append(scanner.nextLine()).append("\n");
                            isFirst = false;
                        } else {
                            treeSB
                                    .append(prefix)
                                    .append(repeat(' ', stepStringLength))
                                    .append(hasChild ? "|" : EMPTY)
                                    .append(repeat(' ', costPrefixLength))
                                    .append(scanner.nextLine())
                                    .append("\n");
                        }
                    }
                } finally {
                    scanner.close();
                }

            }

        });

        return treeSB.toString();
    }

    /**
     * recursive visit
     */
    private void recursive(int deep, boolean isLast, String prefix, Node node, Callback callback) {
        callback.callback(deep, isLast, prefix, node);
        if (!node.isLeaf()) {
            final int size = node.children.size();
            for (int index = 0; index < size; index++) {
                final boolean isLastFlag = index == size - 1;
                final String currentPrefix = isLast ? prefix + STEP_EMPTY_BOARD : prefix + STEP_HAS_BOARD;
                recursive(
                        deep + 1,
                        isLastFlag,
                        currentPrefix,
                        node.children.get(index),
                        callback
                );
            }
        }
    }

    public boolean isTop() {
        return current.isRoot();
    }

    /**
     * create a branch node
     *
     * @param data node data
     * @return this
     */
    public TTree begin(Object data) {
        current = new Node(current, data);
        current.markBegin();
        return this;
    }

    public TTree begin() {
        return begin(null);
    }

    public Object get() {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        return current.data;
    }

    public TTree set(Object data) {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.data = data;
        return this;
    }

    /**
     * end a branch node
     *
     * @return this
     */
    public TTree end() {
        if (current.isRoot()) {
            throw new IllegalStateException("current node is root.");
        }
        current.markEnd();
        current = current.parent;
        return this;
    }


    /**
     * tree node
     */
    private static class Node {

        /**
         * parent node
         */
        final Node parent;

        /**
         * node data
         */
        Object data;

        /**
         * child nodes
         */
        final List<Node> children = new ArrayList<Node>();

        /**
         * begin timestamp
         */
        private long beginTimestamp;

        /**
         * end timestamp
         */
        private long endTimestamp;

        /**
         * construct root node
         */
        private Node(Object data) {
            this.parent = null;
            this.data = data;
        }

        /**
         * construct a regular node
         *
         * @param parent parent node
         * @param data   node data
         */
        private Node(Node parent, Object data) {
            this.parent = parent;
            this.data = data;
            parent.children.add(this);
        }

        /**
         * is the current node the root node
         *
         * @return true / false
         */
        boolean isRoot() {
            return null == parent;
        }

        /**
         * if the current node the leaf node
         *
         * @return true / false
         */
        boolean isLeaf() {
            return children.isEmpty();
        }

        Node markBegin() {
            beginTimestamp = currentTimeMillis();
            return this;
        }

        Node markEnd() {
            endTimestamp = currentTimeMillis();
            return this;
        }

    }


    /**
     * callback interface for recursive visit
     */
    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, Node node);

    }

}
