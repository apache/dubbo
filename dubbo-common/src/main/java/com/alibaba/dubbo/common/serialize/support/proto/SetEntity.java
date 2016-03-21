package com.alibaba.dubbo.common.serialize.support.proto;

import java.util.Set;

public class SetEntity<T> {
	private Set<T> data;
	
	public SetEntity(){
		
	}
	
	public SetEntity(Set<T> data){
		this.data = data;
	}

	public Set<T> getData() {
		return data;
	}

	public void setData(Set<T> data) {
		this.data = data;
	}
	
	
	
	
}
