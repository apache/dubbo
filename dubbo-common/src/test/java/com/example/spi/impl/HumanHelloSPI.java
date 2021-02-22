package com.example.spi.impl;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.wrapper.HelloSPI;

@Activate(group = { "comsumer2" })
public class HumanHelloSPI implements HelloSPI {

	@Override
	public void say(String say) {
		System.out.println("human say:" + say);
	}

}
