/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-4-15
 * $Id: ConsumerService.java 182013 2012-06-26 10:32:43Z tony.chenl $
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.service;

import com.alibaba.dubbo.registry.common.domain.Consumer;

import java.util.List;

/**
 * 消费者数据访问对象
 *
 * @author william.liangf
 */
public interface ConsumerService {

    List<Consumer> findByService(String serviceName);

    Consumer findConsumer(Long id);

    List<Consumer> findAll();

    /**
     * 查询所有的消费者地址
     */
    List<String> findAddresses();

    List<String> findAddressesByApplication(String application);

    List<String> findAddressesByService(String serviceName);

    List<Consumer> findByAddress(String consumerAddress);

    List<String> findServicesByAddress(String consumerAddress);

    List<String> findApplications();

    List<String> findApplicationsByServiceName(String serviceName);

    List<Consumer> findByApplication(String application);

    List<String> findServicesByApplication(String application);

    List<String> findServices();

}