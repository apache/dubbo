package com.service;

import com.pojo.Demo1;
import com.pojo.Demo2;
import com.pojo.Demo4;
import com.pojo.Demo5;
import com.pojo.Demo6;
import com.pojo.Demo7;
import com.pojo.Demo8;
import com.pojo.DemoException1;
import com.pojo.DemoException3;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public interface DemoService<T extends Demo8> {
    Demo1 getDemo1();

    void setDemo2(Demo2 demo2);

    List<Demo4> getDemo4s();

    List<HashSet<LinkedList<Set<Vector<Map<? extends Demo5, ? super Demo6>>>>>> getDemo5s();

    List<Demo7>[] getDemo7s();

    List<T> getTs();

    void echo1() throws DemoException1;

    void echo2() throws DemoException3;
}
