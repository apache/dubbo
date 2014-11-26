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
package com.alibaba.dubbo.examples.jackson.api;

/**
 * @author william.liangf
 */
public interface JacksonService {
    
    String sayHello(String name);

    public JacksonBean testJacksonBean(JacksonBean jacksonBean, JacksonInnerBean jacksonInnerBean);

    public Inherit testInheritBean(Inherit inherit, JacksonBean jacksonBean);

    public int[] testArray(int[] array);

    public JacksonBean[] testBeanArray(JacksonBean[] jacksonBeans);

    public void testException();
}
