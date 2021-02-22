package org.apache.dubbo.common.extension.wrapper.impl;



import org.apache.dubbo.common.extension.wrapper.HelloSPI;

public class WrapperHelloSPI implements HelloSPI {

	private final HelloSPI spi;

	public WrapperHelloSPI(HelloSPI spi) {
		this.spi = spi;
	}

	@Override
	public void say(String say) {
		spi.say("wapper:-->"+say);
	}

}
