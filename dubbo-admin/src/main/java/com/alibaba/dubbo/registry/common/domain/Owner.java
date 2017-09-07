package com.alibaba.dubbo.registry.common.domain;

public class Owner extends Entity {

    private static final long serialVersionUID = -4891350118145794727L;

    /**
     * 可以包含通配符。
     */
    private String service;

    private String username;

    private User user;

    public Owner() {
    }

    public Owner(Long id) {
        super(id);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
