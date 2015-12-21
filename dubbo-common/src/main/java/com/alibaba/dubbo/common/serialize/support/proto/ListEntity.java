package com.alibaba.dubbo.common.serialize.support.proto;

import java.util.List;

public class ListEntity<T> {
	private List<T> data;
	
	public ListEntity(){
		
	}
	
	public ListEntity(List<T> data){
		this.data = data;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}
	
	
	
	
}
