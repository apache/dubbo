package com.alibaba.dubbo.common.serialize.serialization.proto;

import java.io.Serializable;
import java.util.List;

public class ListEnum implements Serializable{
	private String smallName;
	private Integer smallId;


	
	public String getSmallName() {
		return smallName;
	}



	public void setSmallName(String smallName) {
		this.smallName = smallName;
	}



	public Integer getSmallId() {
		return smallId;
	}



	public void setSmallId(Integer smallId) {
		this.smallId = smallId;
	}



	@Override
	public String toString(){
		return "name: " + smallName + " id: " + smallId;
	}
}
