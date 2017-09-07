/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-4-9
 * $Id: Change.java 181192 2012-06-21 05:05:47Z tony.chenl $
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
package com.alibaba.dubbo.registry.common.domain;

/**
 * 服务变更信息对象
 *
 * @author rain.chenjr
 */
public class Change extends Entity {

    public static final String PROVIDER_TYPE = "P";     //服务提供变更
    public static final String CONSUMER_TYPE = "N";     //服务消费者变更
    public static final String ROUTE_TYPE = "R";        //路由变更
    public static final String WEIGHT_TYPE = "W";       //权重变更
    public static final String LOADBALANCE_TYPE = "L";  //负载均衡变更
    public static final String CLUSTER_TYPE = "G";      //分组变更
    public static final String USER_TYPE = "U";         //用户变更
    public static final String CONFIG_TYPE = "C";       //系统配置变更
    public static final String FEATURE_TYPE = "F";      //系统功能变更
    public static final String LAYER_TYPE = "Y";      //系统功能变更
    public static final String TEST_TYPE = "T";         //服务测试变更
    public static final String MOCK_TYPE = "M";         //服务测试变更
    public static final String ACCESS_TYPE = "A";       //服务访问控制变更
    public static final String OVERRIDE_TYPE = "O";     //参数覆盖变更
    private static final long serialVersionUID = 15528419903956898L;
    private String type;                    /* 变更类型 */

    private String service;                 /* 服务名称 */

    private long sequence;                /* 变更序号 */

    private String data;                    /* 变更内容 */

    public Change() {
    }

    public Change(Long id) {
        super(id);
    }

    public Change(String type, String serviceName) {
        this.type = type;
        this.service = serviceName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Deprecated
    /**
     * 用change的id作为sequence
     */
    public long getSequence() {
        return sequence;
    }

    @Deprecated
    /**
     * 用change的id作为sequence
     */
    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
