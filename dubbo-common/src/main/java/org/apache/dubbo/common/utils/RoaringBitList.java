package org.apache.dubbo.common.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.roaringbitmap.IntIterator;
import org.roaringbitmap.RoaringBitmap;

class RoaringBitList<E> implements BitListInterf<E> {
    private final RoaringBitmap rootMap;
    private final List<E> unmodifiableList;

    // FIXME 看一下高版本的RoaringBitMap，是否有内置函数支持更高效的遍历
    RoaringBitList(List<E> unmodifiableList, boolean empty) {
        this.unmodifiableList = unmodifiableList;
        this.rootMap = new RoaringBitmap();
        if (!empty) {
            this.rootMap.add(0L, unmodifiableList.size());
        }
    }

    private RoaringBitList(List<E> unmodifiableList, RoaringBitmap rootMap) {
        this.unmodifiableList = unmodifiableList;
        this.rootMap = rootMap;
    }

    public RoaringBitList(List<E> unmodifiableList) {
        this(unmodifiableList, false);
    }

    public List<E> getUnmodifiableList() {
        return unmodifiableList;
    }

    public void addIndex(int index) {
        this.rootMap.add(index);
    }

    @Override
    public BitListInterf<E> intersect(BitList<E> b, List<E> totalList) {
        RoaringBitmap resultMap = rootMap.clone();
        resultMap.and(((RoaringBitList) b.delegate).rootMap);
        return new RoaringBitList<>(totalList, resultMap);
    }

    @Override
    public int size() {
        return rootMap.getCardinality();
    }

    @Override
    public boolean isEmpty() {
        return rootMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        int idx = unmodifiableList.indexOf(o);
        return idx >= 0 && rootMap.contains(idx);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int prev = -1;

            @Override
            public boolean hasNext() {
                return -1 != rootMap.nextValue(prev + 1);
            }

            @Override
            public E next() {
                prev = (int) rootMap.nextValue(prev + 1);
                return unmodifiableList.get(prev);
            }

            @Override
            public void remove() {
                rootMap.remove(prev);
            }
        };
    }

    @Override
    public Object[] toArray() {
        int size = size();
        Object[] obj = new Object[size];
        for (int i = 0; i < size; i++) {
            obj[i] = unmodifiableList.get(rootMap.select(i));
        }
        return obj;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int size = size();
        Object[] arr = toArray();
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(arr, size, a.getClass());
        System.arraycopy(arr, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return null;
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        int idx = unmodifiableList.indexOf(o);
        if (idx > -1) {
            rootMap.remove(idx);
        }
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        for (Object o : c) {
            remove(o);
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        rootMap.clear();
    }

    @Override
    public E get(int index) {
        int real = rootMap.select(index);
        return unmodifiableList.get(real);
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        rootMap.remove(index);
        return null;
    }

    @Override
    public int indexOf(Object o) {
        IntIterator intIterator = rootMap.getIntIterator();
        int st = 0;
        while (intIterator.hasNext()) {
            int idxInMap = intIterator.next();
            if (unmodifiableList.get(idxInMap).equals(o)) {
                return st;
            }
            st++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }


    @Override
    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }


    @Override
    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }


}
