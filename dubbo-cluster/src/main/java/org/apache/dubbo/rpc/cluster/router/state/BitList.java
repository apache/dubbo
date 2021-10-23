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
package org.apache.dubbo.rpc.cluster.router.state;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * BitList based on BitMap implementation.
 *
 * @param <E>
 * @since 3.0
 */
public class BitList<E> extends AbstractList<E> {
    private final BitSet rootSet;
    private final List<E> unmodifiableList;

    public BitList(List<E> unmodifiableList) {
        this(unmodifiableList, false);
    }

    public BitList(List<E> unmodifiableList, boolean empty) {
        this.unmodifiableList = new ArrayList<>(unmodifiableList);
        this.rootSet = new BitSet();
        if (!empty) {
            this.rootSet.set(0, unmodifiableList.size());
        }
    }

    public BitList(List<E> unmodifiableList, BitSet rootSet) {
        this.unmodifiableList = new ArrayList<>(unmodifiableList);
        this.rootSet = rootSet;
    }

    // Provided by BitList only
    public List<E> getUnmodifiableList() {
        return unmodifiableList;
    }

    public void addIndex(int index) {
        this.rootSet.set(index);
    }

    public BitList<E> intersect(List<E> b, List<E> totalList) {
        BitSet resultSet = (BitSet) rootSet.clone();
        resultSet.and(((BitList) b).rootSet);
        return new BitList<>(totalList, resultSet);
    }

    public BitList<E> and(BitList<E> b) {
        BitSet resultSet = (BitSet) rootSet.clone();
        resultSet.and(b.rootSet);
        return new BitList<>(unmodifiableList, resultSet);
    }

    // Provided by JDK List interface
    @Override
    public int size() {
        return rootSet.cardinality();
    }

    @Override
    public boolean contains(Object o) {
        int idx = unmodifiableList.indexOf(o);
        return idx >= 0 && rootSet.get(idx);
    }

    @Override
    public Iterator<E> iterator() {
        return new BitListIterator<>(this, 0);
    }

    @Override
    public boolean add(E e) {
        int index = unmodifiableList.indexOf(e);
        if (index > -1) {
            rootSet.set(index);
            return true;
        } else {
            throw new UnsupportedOperationException("BitList only support adding element which is the element of origin list.");
        }
    }

    @Override
    public boolean remove(Object o) {
        int idx = unmodifiableList.indexOf(o);
        if (idx > -1 && rootSet.get(idx)) {
            rootSet.set(idx, false);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        rootSet.clear();
        // to remove references
        unmodifiableList.clear();
    }

    @Override
    public E get(int index) {
        int bitIndex = -1;
        for (int i = 0; i <= index; i++) {
            bitIndex = rootSet.nextSetBit(bitIndex + 1);
            if (bitIndex == -1) {
                return null;
            }
        }
        return unmodifiableList.get(bitIndex);
    }

    @Override
    public E remove(int index) {
        int bitIndex = -1;
        for (int i = 0; i <= index; i++) {
            bitIndex = rootSet.nextSetBit(bitIndex + 1);
            if (bitIndex == -1) {
                return null;
            }
        }
        rootSet.set(index, false);
        return unmodifiableList.get(bitIndex);
    }

    @Override
    public int indexOf(Object o) {
        int bitIndex = -1;
        for (int i = 0; i < size(); i++) {
            bitIndex = rootSet.nextSetBit(bitIndex + 1);
            if (unmodifiableList.get(bitIndex).equals(o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int bitIndex = -1;
        int index = -1;
        for (int i = 0; i < size(); i++) {
            bitIndex = rootSet.nextSetBit(bitIndex + 1);
            if (unmodifiableList.get(bitIndex).equals(o)) {
                index = i;
            }
        }
        return index;
    }

    @Override
    public ListIterator<E> listIterator() {
        return new BitListIterator<>(this, 0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new BitListIterator<>(this, index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        BitSet resultSet = (BitSet) rootSet.clone();
        if (fromIndex > 0) {
            resultSet.set(0, fromIndex, false);
        }
        if (toIndex < resultSet.length()) {
            resultSet.set(toIndex, resultSet.length(), false);
        }
        return new BitList<>(unmodifiableList, resultSet);
    }

    public static class BitListIterator<E> implements ListIterator<E> {
        private BitList<E> bitList;
        private int index;
        private int curBitIndex = -1;

        public BitListIterator(BitList<E> bitList, int index) {
            this.bitList = bitList;
            this.index = index - 1;
            for (int i = 0; i < index; i++) {
                curBitIndex = bitList.rootSet.nextSetBit(curBitIndex + 1);
            }
        }

        @Override
        public boolean hasNext() {
            return -1 != bitList.rootSet.nextSetBit(curBitIndex + 1);
        }

        @Override
        public E next() {
            curBitIndex = bitList.rootSet.nextSetBit(curBitIndex + 1);
            index += 1;
            return bitList.unmodifiableList.get(curBitIndex);
        }

        @Override
        public boolean hasPrevious() {
            return curBitIndex != -1 && bitList.rootSet.previousSetBit(curBitIndex - 1) > -1;
        }

        @Override
        public E previous() {
            curBitIndex = bitList.rootSet.previousSetBit(curBitIndex - 1);
            index -= 1;
            return bitList.unmodifiableList.get(curBitIndex);
        }

        @Override
        public int nextIndex() {
            return hasNext() ? index + 1 : index;
        }

        @Override
        public int previousIndex() {
            return hasPrevious() ? index - 1: index;
        }

        @Override
        public void remove() {
            bitList.rootSet.set(curBitIndex, false);
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException("Set method is not supported in BitListIterator!");
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException("Add method is not supported in BitListIterator!");
        }
    }

    @Override
    public BitList<E> clone() {
        return new BitList<>(unmodifiableList, (BitSet) rootSet.clone());
    }
}
