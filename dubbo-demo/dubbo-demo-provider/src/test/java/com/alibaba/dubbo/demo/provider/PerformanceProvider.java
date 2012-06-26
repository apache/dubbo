package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

public class PerformanceProvider {

	public static void main(String[] args) throws Exception {
		RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
		final Registry registry = registryFactory.getRegistry(URL.valueOf("zookeeper://10.20.153.10:2181"));
		for (int i = 0; i < 10; i ++) {
			final int fi = i;
			new Thread(new Runnable() {
				public void run() {
					for (int j = 0; j < 10; j ++) {
						registry.register(URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":208" + fi + "/com.alibaba.dubbo.demo.DemoService" + j + "?version=1.0.0&timeout=2000"));
					}
				}
			}).start();
		}
		System.in.read();
	}

}
