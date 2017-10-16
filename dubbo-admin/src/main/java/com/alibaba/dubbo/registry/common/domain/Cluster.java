package com.alibaba.dubbo.registry.common.domain;

public class Cluster extends Entity {

    private static final long serialVersionUID = 8704571999015097948L;

    private String name;   /* 服务提供者分组名 ，一个分组可以包含若干个提供者*/

    private String address;     /* 客户端地址 */

    private String username;

    public Cluster() {
    }

    public Cluster(Long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}
