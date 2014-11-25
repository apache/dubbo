/**
 * Project: dubbo-examples
 *
 * File Created at 2012-2-17
 * $Id$
 *
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.examples.jackson.impl;

import com.alibaba.dubbo.examples.jackson.api.*;

/**
 * @author william.liangf
 */
public class JacksonServiceImpl implements JacksonService {

    public String sayHello(String name) {
        return "hello, " + name;
    }

    @Override
    public JacksonBean testJacksonBean(JacksonBean jacksonBean, JacksonInnerBean jacksonInnerBean) {
        System.out.println(jacksonBean);
        System.out.println(jacksonInnerBean);
        jacksonBean.getInnerBeanList().add(jacksonInnerBean);
        return jacksonBean;
    }

    @Override
    public Inherit testInheritBean(Inherit inherit, JacksonBean jacksonBean) {
        System.out.println(inherit);
        System.out.println(jacksonBean);
        return new InheritBean2();
    }

    @Override
    public int[] testArray(int[] array) {
        return new int[]{3, 4};
    }

    @Override
    public JacksonBean[] testBeanArray(JacksonBean[] jacksonBeans) {
        System.out.println("testBeanArray");
        for (JacksonBean in : jacksonBeans) {
            System.out.println(in);
        }
        return new JacksonBean[]{
                new JacksonBean(), new JacksonBean()
        };
    }

    @Override
    public void testException() {
        throw new RuntimeException("exception from provider");
    }
}
