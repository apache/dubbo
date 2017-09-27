package com.alibaba.dubbo.registry.common.domain;

public class SearchHistory extends Entity {

    private static final long serialVersionUID = -1281982267153430266L;

    private String name;

    private String type;

    private String url;

    public SearchHistory() {
    }

    public SearchHistory(Long id) {
        super(id);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
