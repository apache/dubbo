package com.alibaba.dubbo.demo.consumer;

import java.util.List;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

public class PerformanceConsumer {

	public static void main(String[] args) throws Exception {
		RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
		final Registry registry = registryFactory.getRegistry(URL.valueOf("zookeeper://10.20.153.10:2181"));
		for (int i = 0; i < 10; i ++) {
			final int fi = i;
			new Thread(new Runnable() {
				public void run() {
					for (int j = 0; j < 10; j ++) {
						registry.subscribe(URL.valueOf("consumer://" + NetUtils.getLocalHost() + ":208" + fi + "/com.alibaba.dubbo.demo.DemoService" + j + "?version=1.0.0&timeout=200&category=providers,routers,overrides"),
								new NotifyListener() {
									public void notify(List<URL> urls) {
										System.out.println(urls);
									}
								});
					}
				}
			}).start();
		}
		System.in.read();
	}

}
