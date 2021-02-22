package org.apache.dubbo.common.extension.wrapper;



import org.apache.dubbo.common.extension.SPI;

@SPI
public interface HelloSPI {

	public void say(String say);

}
