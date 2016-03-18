/**
 * 
 */
package com.netease.learn;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author hzjiangwei15
 *
 */
public class MyProxyTest {

	public static void main(String[] args) {
		
		
		InvocationHandler invoker = new InvocationHandler() {
			
			private UserService userService = new UserServiceImpl();
			
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return method.invoke(userService, args);
			}
		};
		
		UserService proxy = (UserService)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{UserService.class}, invoker);
		
		System.out.println(proxy.getName(123));
	}
}

interface UserService{
	
	public String getName(long id);
}

class UserServiceImpl implements UserService{

	@Override
	public String getName(long id) {
		return id + " hello!";
	}
}
