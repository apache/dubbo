package com.alibaba.dubbo.examples.nospring;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.examples.nospring.api.NoSpringService;
import com.alibaba.dubbo.examples.nospring.impl.NoSpringServiceImpl;

public class NoSpringProvider {

	/**
	 *
	 * port = args[0]
	 * group = args[1]
	 *
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		// 服务实现
		NoSpringService xxxService = new NoSpringServiceImpl();
		 
		// 当前应用配置
		ApplicationConfig application = new ApplicationConfig();
		application.setName("nospring-provider");
		 
		// 连接注册中心配置
		RegistryConfig registry = new RegistryConfig();
//		registry.setAddress("multicast://224.5.6.7:1234");
		registry.setAddress("zookeeper://127.0.0.1:2181");
		registry.setUsername("aaa");
		registry.setPassword("bbb");
//		registry.setGroup("group1");
		 
		// 服务提供者协议配置
		ProtocolConfig protocol = new ProtocolConfig();
		protocol.setName("dubbo");
		protocol.setPort(Integer.parseInt(args[0]));
		protocol.setThreads(100);
		 
		// 注意：ServiceConfig为重对象，内部封装了与注册中心的连接，以及开启服务端口
		// 服务提供者暴露服务配置
		// 此实例很重，封装了与注册中心的连接，请自行缓存，否则可能造成内存和连接泄漏
		ServiceConfig<NoSpringService> service = new ServiceConfig<NoSpringService>(); 
		service.setApplication(application);
		service.setRegistry(registry); // 多个注册中心可以用setRegistries()
		service.setProtocol(protocol); // 多个协议可以用setProtocols()
		service.setInterface(NoSpringService.class);
		service.setRef(xxxService);
		service.setVersion("1.0.0");
		service.setGroup(args[1]);

		// 暴露及注册服务
		service.export();
		
		Thread.sleep(1000*1000000);
	}
}
