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

import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * BitList based on BitMap implementation.
 * BitList is consists of `originList`, `rootSet` and `tailList`.
 * <p>
 * originList: Initial elements of the list. This list will not be changed
 * in modification actions (expect clear all).
 * rootSet: A bitMap to store the indexes of originList are still exist.
 * Most of the modification actions are operated on this bitMap.
 * tailList: An additional list for BitList. Worked when adding totally new
 * elements to list. These elements will be appended to the last
 * of the BitList.
 * <p>
 * An example of BitList:
 * originList:  A  B  C  D  E             (5 elements)
 * rootSet:     x  v  x  v  v
 * 0  1  0  1  1             (5 elements)
 * tailList:                   F  G  H    (3 elements)
 * resultList:     B     D  E  F  G  H    (6 elements)
 *
 * @param <E>
 * @since 3.0
 */
public class BitList<E> extends AbstractList<E> {
    private final BitSet rootSet;
    private volatile List<E> originList;
    private final static BitList emptyList = new BitList(Collections.emptyList());
    private volatile List<E> tailList = null;

    public BitList(List<E> originList) {
        this(originList, false);
    }

    public BitList(List<E> originList, boolean empty) {
        if (originList instanceof BitList) {
            this.originList = ((BitList<E>) originList).getOriginList();
            this.tailList = ((BitList<E>) originList).getTailList();
        } else {
            this.originList = originList;
        }
        this.rootSet = new BitSet();
        if (!empty) {
            this.rootSet.set(0, originList.size());
        } else {
            this.tailList = null;
        }
    }

    public BitList(List<E> originList, boolean empty, List<E> tailList) {
        this.originList = originList;
        this.rootSet = new BitSet();
        if (!empty) {
            this.rootSet.set(0, originList.size());
        }
        this.tailList = tailList;
    }

    public BitList(List<E> originList, BitSet rootSet, List<E> tailList) {
        this.originList = originList;
        this.rootSet = rootSet;
        this.tailList = tailList;
    }

    // Provided by BitList only
    public List<E> getOriginList() {
        return originList;
    }

    public void addIndex(int index) {
        this.rootSet.set(index);
    }

    public int totalSetSize() {
        return this.originList.size();
    }

    public boolean indexExist(int index) {
        return this.rootSet.get(index);
    }

    public E getByIndex(int index) {
        return this.originList.get(index);
    }

    /**
     * And operation between two bitList. Return a new cloned list.
     * TailList in source bitList will be totally saved even if it is not appeared in the target bitList.
     *
     * @param target target bitList
     * @return a new bitList only contains those elements contain in both two list and source bitList's tailList
     */
    public BitList<E> and(BitList<E> target) {
        rootSet.and(target.rootSet);
        return this;
    }

    public BitList<E> or(BitList<E> target) {
        BitSet resultSet = (BitSet) rootSet.clone();
        resultSet.or(target.rootSet);
        return new BitList<>(originList, resultSet, tailList);
    }

    public boolean hasMoreElementInTailList() {
        return CollectionUtils.isNotEmpty(tailList) && tailList.size() > 0;
    }

    public List<E> getTailList() {
        return tailList;
    }

    public void addToTailList(E e) {
        if (tailList == null) {
            tailList = new LinkedList<>();
        }
        tailList.add(e);
    }

    public E randomSelectOne() {
        int originSize = originList.size();
        int tailSize = tailList != null ? tailList.size() : 0;
        int totalSize = originSize + tailSize;
        int cardinality = rootSet.cardinality();

        // example 1 : origin size is 1000, cardinality is 50, rate is 1/20. 20 * 2 = 40 < 50, try random select
        // example 2 : origin size is 1000, cardinality is 25, rate is 1/40. 40 * 2 = 80 > 50, directly use iterator
        int rate = originSize / cardinality;
        if (rate <= cardinality * 2) {
            int count = rate * 5;
            for (int i = 0; i < count; i++) {
                int random = ThreadLocalRandom.current().nextInt(totalSize);
                if (random < originSize) {
                    if (rootSet.get(random)) {
                        return originList.get(random);
                    }
                } else {
                    return tailList.get(random - originSize);
                }
            }
        }
        return get(ThreadLocalRandom.current().nextInt(cardinality + tailSize));
    }

    @SuppressWarnings("unchecked")
    public static <T> BitList<T> emptyList() {
        return emptyList;
    }

    // Provided by JDK List interface
    @Override
    public int size() {
        return rootSet.cardinality() + (CollectionUtils.isNotEmpty(tailList) ? tailList.size() : 0);
    }

    @Override
    public boolean contains(Object o) {
        int idx = originList.indexOf(o);
        return (idx >= 0 && rootSet.get(idx)) || (CollectionUtils.isNotEmpty(tailList) && tailList.contains(o));
    }

    @Override
    public Iterator<E> iterator() {
        return new BitListIterator<>(this, 0);
    }

    /**
     * If the element to added is appeared in originList even if it is not in rootSet,
     * directly set its index in rootSet to true. (This may change the order of elements.)
     * <p>
     * If the element is not contained in originList, allocate tailList and add to tailList.
     * <p>
     * Notice: It is not recommended adding duplicated element.
     */
    @Override
    public boolean add(E e) {
        int index = originList.indexOf(e);
        if (index > -1) {
            rootSet.set(index);
            return true;
        } else {
            if (tailList == null) {
                tailList = new LinkedList<>();
            }
            return tailList.add(e);
        }
    }

    /**
     * If the element to added is appeared in originList,
     * directly set its index in rootSet to false. (This may change the order of elements.)
     * <p>
     * If the element is not contained in originList, try to remove from tailList.
     */
    @Override
    public boolean remove(Object o) {
        int idx = originList.indexOf(o);
        if (idx > -1 && rootSet.get(idx)) {
            rootSet.set(idx, false);
            return true;
        }
        if (CollectionUtils.isNotEmpty(tailList)) {
            return tailList.remove(o);
        }
        return false;
    }

    /**
     * Caution: This operation will clear originList for removing references purpose.
     * This may change the default behaviour when adding new element later.
     */
    @Override
    public void clear() {
        rootSet.clear();
        // to remove references
        originList = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(tailList)) {
            tailList = null;
        }
    }

    @Override
    public E get(int index) {
        int bitIndex = -1;
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (index >= rootSet.cardinality()) {
            if (CollectionUtils.isNotEmpty(tailList)) {
                return tailList.get(index - rootSet.cardinality());
            } else {
                throw new IndexOutOfBoundsException();
            }
        } else {
            for (int i = 0; i <= index; i++) {
                bitIndex = rootSet.nextSetBit(bitIndex + 1);
            }
            return originList.get(bitIndex);
        }
    }

    @Override
    public E remove(int index) {
        int bitIndex = -1;
        if (index >= rootSet.cardinality()) {
            if (CollectionUtils.isNotEmpty(tailList)) {
                return tailList.remove(index - rootSet.cardinality());
            } else {
                throw new IndexOutOfBoundsException();
            }
        } else {
            for (int i = 0; i <= index; i++) {
                bitIndex = rootSet.nextSetBit(bitIndex + 1);
            }
            rootSet.set(bitIndex, false);
            return originList.get(bitIndex);
        }
    }

    @Override
    public int indexOf(Object o) {
        int bitIndex = -1;
        for (int i = 0; i < rootSet.cardinality(); i++) {
            bitIndex = rootSet.nextSetBit(bitIndex + 1);
            if (originList.get(bitIndex).equals(o)) {
                return i;
            }
        }
        if (CollectionUtils.isNotEmpty(tailList)) {
            int indexInTailList = tailList.indexOf(o);
            if (indexInTailList != -1) {
                return indexInTailList + rootSet.cardinality();
            } else {
                return -1;
            }
        }
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean addAll(Collection<? extends E> c) {
        if (c instanceof BitList) {
            rootSet.or(((BitList<? extends E>) c).rootSet);
            if (((BitList<? extends E>) c).hasMoreElementInTailList()) {
                for (E e : ((BitList<? extends E>) c).tailList) {
                    addToTailList(e);
                }
            }
            return true;
        }
        return super.addAll(c);
    }

    @Override
    public int lastIndexOf(Object o) {
        int bitIndex = -1;
        int index = -1;
        if (CollectionUtils.isNotEmpty(tailList)) {
            int indexInTailList = tailList.lastIndexOf(o);
            if (indexInTailList > -1) {
                return indexInTailList + rootSet.cardinality();
            }
        }
        for (int i = 0; i < rootSet.cardinality(); i++) {
            bitIndex = rootSet.nextSetBit(bitIndex + 1);
            if (originList.get(bitIndex).equals(o)) {
                index = i;
            }
        }
        return index;
    }

    @Override
    public boolean isEmpty() {
        return this.rootSet.isEmpty() && (tailList == null || tailList.isEmpty());
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
    public BitList<E> subList(int fromIndex, int toIndex) {
        BitSet resultSet = (BitSet) rootSet.clone();
        List<E> copiedTailList = tailList == null ? null : new LinkedList<>(tailList);
        if (toIndex < size()) {
            if (toIndex < rootSet.cardinality()) {
                copiedTailList = null;
                resultSet.set(toIndex, resultSet.length(), false);
            } else {
                copiedTailList = copiedTailList == null ? null : copiedTailList.subList(0, toIndex - rootSet.cardinality());
            }
        }
        if (fromIndex > 0) {
            if (fromIndex < rootSet.cardinality()) {
                resultSet.set(0, fromIndex, false);
            } else {
                resultSet.clear();
                copiedTailList = copiedTailList == null ? null : copiedTailList.subList(fromIndex - rootSet.cardinality(), copiedTailList.size());
            }
        }
        return new BitList<>(originList, resultSet, copiedTailList);
    }

    public static class BitListIterator<E> implements ListIterator<E> {
        private BitList<E> bitList;
        private int index;
        private ListIterator<E> tailListIterator;
        private int curBitIndex = -1;
        private boolean isInTailList = false;
        private int lastReturnedIndex = -1;

        public BitListIterator(BitList<E> bitList, int index) {
            this.bitList = bitList;
            this.index = index - 1;
            for (int i = 0; i < index; i++) {
                if (!isInTailList) {
                    curBitIndex = bitList.rootSet.nextSetBit(curBitIndex + 1);
                    if (curBitIndex == -1) {
                        if (CollectionUtils.isNotEmpty(bitList.tailList)) {
                            isInTailList = true;
                            tailListIterator = bitList.tailList.listIterator();
                            tailListIterator.next();
                        } else {
                            break;
                        }
                    }
                } else {
                    tailListIterator.next();
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (isInTailList) {
                return tailListIterator.hasNext();
            } else {
                int nextBit = bitList.rootSet.nextSetBit(curBitIndex + 1);
                if (nextBit == -1) {
                    return bitList.hasMoreElementInTailList();
                } else {
                    return true;
                }
            }
        }

        @Override
        public E next() {
            if (isInTailList) {
                if (tailListIterator.hasNext()) {
                    index += 1;
                    lastReturnedIndex = index;
                }
                return tailListIterator.next();
            } else {
                int nextBitIndex = bitList.rootSet.nextSetBit(curBitIndex + 1);
                if (nextBitIndex == -1) {
                    if (bitList.hasMoreElementInTailList()) {
                        tailListIterator = bitList.tailList.listIterator();
                        isInTailList = true;
                        index += 1;
                        lastReturnedIndex = index;
                        return tailListIterator.next();
                    } else {
                        throw new NoSuchElementException();
                    }
                } else {
                    index += 1;
                    lastReturnedIndex = index;
                    curBitIndex = nextBitIndex;
                    return bitList.originList.get(nextBitIndex);
                }
            }
        }

        @Override
        public boolean hasPrevious() {
            if (isInTailList) {
                boolean hasPreviousInTailList = tailListIterator.hasPrevious();
                if (hasPreviousInTailList) {
                    return true;
                } else {
                    return bitList.rootSet.previousSetBit(bitList.rootSet.size()) != -1;
                }
            } else {
                return curBitIndex != -1;
            }
        }

        @Override
        public E previous() {
            if (isInTailList) {
                boolean hasPreviousInTailList = tailListIterator.hasPrevious();
                if (hasPreviousInTailList) {
                    lastReturnedIndex = index;
                    index -= 1;
                    return tailListIterator.previous();
                } else {
                    int lastIndexInBit = bitList.rootSet.previousSetBit(bitList.rootSet.size());
                    if (lastIndexInBit == -1) {
                        throw new NoSuchElementException();
                    } else {
                        isInTailList = false;
                        curBitIndex = bitList.rootSet.previousSetBit(lastIndexInBit - 1);
                        lastReturnedIndex = index;
                        index -= 1;
                        return bitList.originList.get(lastIndexInBit);
                    }
                }
            } else {
                if (curBitIndex == -1) {
                    throw new NoSuchElementException();
                }
                int nextBitIndex = curBitIndex;
                curBitIndex = bitList.rootSet.previousSetBit(curBitIndex - 1);
                lastReturnedIndex = index;
                index -= 1;
                return bitList.originList.get(nextBitIndex);
            }
        }

        @Override
        public int nextIndex() {
            return hasNext() ? index + 1 : index;
        }

        @Override
        public int previousIndex() {
            return index;
        }

        @Override
        public void remove() {
            if (lastReturnedIndex == -1) {
                throw new IllegalStateException();
            } else {
                if (lastReturnedIndex >= bitList.rootSet.cardinality()) {
                    tailListIterator.remove();
                } else {
                    int bitIndex = -1;
                    for (int i = 0; i <= lastReturnedIndex; i++) {
                        bitIndex = bitList.rootSet.nextSetBit(bitIndex + 1);
                    }
                    bitList.rootSet.set(bitIndex, false);
                }
            }
            if (lastReturnedIndex <= index) {
                index -= 1;
            }
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

    public ArrayList<E> cloneToArrayList() {
        if (rootSet.cardinality() == originList.size() && (tailList == null || tailList.isEmpty())) {
            return new ArrayList<>(originList);
        }
        ArrayList<E> arrayList = new ArrayList<>(size());
        arrayList.addAll(this);
        return arrayList;
    }

    @Override
    public BitList<E> clone() {
        return new BitList<>(originList, (BitSet) rootSet.clone(), tailList == null ? null : new LinkedList<>(tailList));
    }
}
