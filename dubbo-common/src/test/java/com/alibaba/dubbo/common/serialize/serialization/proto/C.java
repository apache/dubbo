package com.alibaba.dubbo.common.serialize.serialization.proto;

import java.util.List;

public class C<T>{
	String name;
	int value;
	List<T> lists;
	
	public List<T> getLists() {
		return lists;
	}
	public void setLists(List<T> lists) {
		this.lists = lists;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
		this.value = value;
	}
	
	@Override
	public String toString(){
		return "name: " + name + " value: " + value + " lists: " + lists;
	}
}
