package com.alibaba.dubbo.common.serialize.serialization.proto;

import java.io.Serializable;
import java.util.List;

public class TestObj implements Serializable{
	private String name;
	private Integer id;
	private List<ListEnum> lists;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	
	public List<ListEnum> getLists() {
		return lists;
	}
	public void setLists(List<ListEnum> lists) {
		this.lists = lists;
	}
	@Override
	public String toString(){
		return "name: " + name + " id: " + id + " lists: " + lists;
	}
}
