package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.DemoBean;
import com.alibaba.dubbo.demo.DemoCacheService;

public class DemoCacheServiceImpl implements DemoCacheService {
	
	@Override
	public DemoBean getBeanById(int id) {
		DemoBean bean = new DemoBean();
		bean.setId(id);
		bean.setName("name_" + id);
		return bean;
	}
	
	@Override
	public DemoBean getBean(DemoBean bean) {
		bean.setName(bean.getName() + " add " + bean.getName());
		return bean;
	}
	
	@Override
	public void setBean(DemoBean bean) {
		
	}

}
