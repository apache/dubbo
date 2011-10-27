package com.alibaba.dubbo.rpc.protocol.hessian;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.dubbo.rpc.service.EchoService;
import com.alibaba.dubbo.rpc.service.GenericService;

/**
 * 
 * 
 * @author qianlei
 */

public class HessianUtils
{
	private final static Method[] FrameworkMethods;

	static
	{
		List<Method> methods = new ArrayList<Method>();

		Class<?> c = EchoService.class;
		for( Method method : c.getDeclaredMethods() )
			methods.add(method);

		c = GenericService.class;
		for( Method method : c.getDeclaredMethods() )
			methods.add(method);

		FrameworkMethods = methods.toArray(new Method[0]);
	}

	private HessianUtils(){}

	public static Method getFrameworkMethod(String name)
	{
		if( name != null )
		{
			for( Method method : FrameworkMethods )
				if( name.startsWith(method.getName()) )
					return method;
		}
		return null;
	}
}
