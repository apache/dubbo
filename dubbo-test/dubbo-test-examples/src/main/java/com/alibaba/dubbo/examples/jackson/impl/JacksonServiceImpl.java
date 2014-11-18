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

import com.alibaba.dubbo.examples.jackson.api.JacksonBean;
import com.alibaba.dubbo.examples.jackson.api.JacksonInnerBean;
import com.alibaba.dubbo.examples.jackson.api.JacksonService;

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
}
