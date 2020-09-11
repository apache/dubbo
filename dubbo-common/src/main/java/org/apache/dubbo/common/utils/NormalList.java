package org.apache.dubbo.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 *
 */
public class NormalList<E> implements BitListInterf<E> {
    private final List<E> unmodifiableList;
    private final List<E> arrayList;

    public NormalList(List<E> unmodifiableList, boolean empty) {
        this.unmodifiableList = unmodifiableList;
        this.arrayList = new ArrayList<>();
        if (!empty) {
            arrayList.addAll(unmodifiableList);
        }
    }

    private NormalList(List<E> unmodifiableList, List<E> arrayList) {
        this.unmodifiableList = unmodifiableList;
        this.arrayList = arrayList;
    }

    @Override
    public void addIndex(int index) {
        arrayList.add(unmodifiableList.get(index));
    }

    @Override
    public BitListInterf<E> intersect(BitList<E> b, List<E> totalList) {
        Set<E> set = new HashSet<>(b);
        ArrayList<E> list = new ArrayList<>();
        for (E e : arrayList) {
            if (set.contains(e)) {
                list.add(e);
            }
        }
        return new NormalList<>(totalList, list);
    }

    @Override
    public List<E> getUnmodifiableList() {
        return unmodifiableList;
    }

    @Override
    public int size() {
        return arrayList.size();
    }

    @Override
    public boolean isEmpty() {
        return arrayList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return arrayList.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return arrayList.iterator();
    }

    @Override
    public Object[] toArray() {
        return arrayList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return arrayList.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return arrayList.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return arrayList.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return arrayList.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return arrayList.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return arrayList.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return arrayList.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return arrayList.retainAll(c);
    }

    @Override
    public void clear() {
        arrayList.clear();
    }

    @Override
    public E get(int index) {
        return arrayList.get(index);
    }

    @Override
    public E set(int index, E element) {
        return arrayList.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        arrayList.add(index, element);
    }

    @Override
    public E remove(int index) {
        return arrayList.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return arrayList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return arrayList.lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return arrayList.listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return arrayList.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return arrayList.subList(fromIndex, toIndex);
    }
}
