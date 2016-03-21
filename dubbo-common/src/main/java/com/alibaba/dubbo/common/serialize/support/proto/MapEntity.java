package com.alibaba.dubbo.common.serialize.support.proto;

import java.util.Map;

public class MapEntity<T, M> {
	private Map<T, M> data;
	
	public MapEntity(){
		
	}
	
	public MapEntity(Map<T, M> data){
		this.data = data;
	}

	public Map<T, M> getData() {
		return data;
	}

	public void setData(Map<T, M> data) {
		this.data = data;
	}
	
	
}
