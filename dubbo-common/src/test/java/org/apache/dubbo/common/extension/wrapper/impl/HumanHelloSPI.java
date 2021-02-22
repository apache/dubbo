package org.apache.dubbo.common.extension.wrapper.impl;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.wrapper.HelloSPI;

@Activate(order = 2, group = "comsumer")
public class HumanHelloSPI implements HelloSPI {

	@Override
	public void say(String say) {
		System.out.println("human say:" + say);
	}

}
