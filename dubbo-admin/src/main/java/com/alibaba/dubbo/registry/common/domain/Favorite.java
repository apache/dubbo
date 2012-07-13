package com.alibaba.dubbo.registry.common.domain;

public class Favorite extends Entity {

	private static final long serialVersionUID = -1281982267153430266L;
	
    private String name;
    
    private String url;
    
    private String username;
    
    public Favorite() {
    }

    public Favorite(Long id) {
        super(id);
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
}
