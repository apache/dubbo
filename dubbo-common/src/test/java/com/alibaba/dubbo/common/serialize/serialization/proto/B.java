package com.alibaba.dubbo.common.serialize.serialization.proto;

public class B{
	String name;
	int value;
	
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
		return "name: " + name + " value: " + value;
	}
}
