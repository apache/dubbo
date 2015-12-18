package com.alibaba.dubbo.examples.nospring.impl;

import com.alibaba.dubbo.examples.nospring.api.NoSpringService;

public class NoSpringServiceImpl implements NoSpringService{

	@Override
	public String sayHello(String name) {
		try {
			Thread.sleep(100);
		} catch (Exception e) {
		}
		System.out.println("call sayHello, time:" + System.currentTimeMillis());
		return name + " hello!";
	}
}
