package com.alibaba.dubbo.qos.textui;

import org.apache.commons.lang3.StringUtils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.repeat;

/**
 * 树形控件
 * Created by oldmanpushcart@gmail.com on 15/5/26.
 */
public class TTree implements TComponent {

    private static final String STEP_FIRST_CHAR = "`---";
    private static final String STEP_NORMAL_CHAR = "+---";
    private static final String STEP_HAS_BOARD = "|   ";
    private static final String STEP_EMPTY_BOARD = "    ";

    // 是否输出耗时
    private final boolean isPrintCost;

    // 根节点
    private final Node root;

    // 当前节点
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
                final int stepStringLength = StringUtils.length(stepString);
                treeSB.append(prefix).append(stepString);

                int costPrefixLength = 0;
                if (hasChild) {
                    treeSB.append("+");
                }
                if (isPrintCost
                        && !node.isRoot()) {
                    final String costPrefix = String.format("[%s,%sms]", (node.endTimestamp - root.beginTimestamp), (node.endTimestamp - node.beginTimestamp));
                    costPrefixLength = StringUtils.length(costPrefix);
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
     * 递归遍历
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
     * 创建一个分支节点
     *
     * @param data 节点数据
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
     * 结束一个分支节点
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
     * 树节点
     */
    private static class Node {

        /**
         * 父节点
         */
        final Node parent;

        /**
         * 节点数据
         */
        Object data;

        /**
         * 子节点
         */
        final List<Node> children = new ArrayList<Node>();

        /**
         * 开始时间戳
         */
        private long beginTimestamp;

        /**
         * 结束时间戳
         */
        private long endTimestamp;

        /**
         * 构造树节点(根节点)
         */
        private Node(Object data) {
            this.parent = null;
            this.data = data;
        }

        /**
         * 构造树节点
         *
         * @param parent 父节点
         * @param data   节点数据
         */
        private Node(Node parent, Object data) {
            this.parent = parent;
            this.data = data;
            parent.children.add(this);
        }

        /**
         * 是否根节点
         *
         * @return true / false
         */
        boolean isRoot() {
            return null == parent;
        }

        /**
         * 是否叶子节点
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
     * 遍历回调接口
     */
    private interface Callback {

        void callback(int deep, boolean isLast, String prefix, Node node);

    }

}
