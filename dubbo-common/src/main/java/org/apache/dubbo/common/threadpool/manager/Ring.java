package org.apache.dubbo.common.threadpool.manager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Ring<T> {

    AtomicInteger count = new AtomicInteger();

    private List<T> itemList = new CopyOnWriteArrayList<T>();

    public void addItem(T t) {
        if (t != null) {
            itemList.add(t);
        }
    }

    public T pollItem() {
        if (itemList.isEmpty()) {
            return null;
        }
        if (itemList.size() == 1) {
            return itemList.get(0);
        }

        if (count.intValue() > Integer.MAX_VALUE - 10000) {
            count.set(count.get() % itemList.size());
        }

        int index = Math.abs(count.getAndIncrement()) % itemList.size();
        return itemList.get(index);
    }

    public T peekItem() {
        if (itemList.isEmpty()) {
            return null;
        }
        if (itemList.size() == 1) {
            return itemList.get(0);
        }
        int index = Math.abs(count.get()) % itemList.size();
        return itemList.get(index);
    }

    public List<T> listItems() {
        return Collections.unmodifiableList(itemList);
    }
}
