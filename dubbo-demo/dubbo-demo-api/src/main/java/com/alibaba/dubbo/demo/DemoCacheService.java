package com.alibaba.dubbo.demo;

public interface DemoCacheService {
	
	public DemoBean getBeanById(int id);
	
	public DemoBean getBean(DemoBean bean);
	
	public void setBean(DemoBean bean);

}
