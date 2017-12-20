package com.alibaba.dubbo.examples.merge.impl;

import com.alibaba.dubbo.examples.merge.api.MergeService;

import java.util.ArrayList;
import java.util.List;


public class MergeServiceImpl implements MergeService {

    public List<String> mergeResult() {
        List<String> menus = new ArrayList<String>();
        menus.add("group-1.1");
        menus.add("group-1.2");
        return menus;
    }

}
