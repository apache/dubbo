package org.apache.dubbo.common.utils;

import java.util.List;

interface BitListInterf<E> extends List<E> {

    void addIndex(int index);

    BitListInterf<E> intersect(BitList<E> b, List<E> totalList);

    List<E> getUnmodifiableList();

}