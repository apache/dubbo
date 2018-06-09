package com.alibaba.dubbo.demo;

import java.io.Serializable;

/**
 * @author luokai
 * @date 2018/5/14
 */
public class SimpleObject implements Serializable {
    private int num;

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    @Override
    public String toString() {
        return "SimpleObject{" +
                "num=" + num +
                '}';
    }
}
