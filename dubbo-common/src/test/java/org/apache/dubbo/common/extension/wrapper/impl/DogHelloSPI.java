package org.apache.dubbo.common.extension.wrapper.impl;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.wrapper.HelloSPI;

@Activate(order = 1)
public class DogHelloSPI implements HelloSPI {

	@Override
	public void say(String say) {
		System.out.println("dog say:" + say);
	}

}
