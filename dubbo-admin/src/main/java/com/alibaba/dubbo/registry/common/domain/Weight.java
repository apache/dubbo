package com.alibaba.dubbo.registry.common.domain;

public class Weight extends Entity {

    //缺省的权重值
    public static final int DEFAULT_WEIGHT = 5;
    private static final long serialVersionUID = -1281982267153430266L;
    private String address; /* 用户配置的提供者地址表达式 */

    private String serviceName;

    private int weight;          /*权重值*/

    private String username;

    public Weight() {
    }

    public Weight(Long id) {
        super(id);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getService() {
        return serviceName;
    }

    public void setService(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
