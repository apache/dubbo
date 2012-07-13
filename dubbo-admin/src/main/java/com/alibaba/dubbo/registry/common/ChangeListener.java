package com.alibaba.dubbo.registry.common;

public interface ChangeListener {

	/**
	 * 数据变更
	 * 
	 * @param type 数据类型
	 * @param services 影响的服务
	 */
	void onChanged(String type);

}
