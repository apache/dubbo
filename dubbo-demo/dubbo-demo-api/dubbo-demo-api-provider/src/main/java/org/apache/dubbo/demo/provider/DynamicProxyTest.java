package org.apache.dubbo.demo.provider;

import org.apache.dubbo.demo.DemoService;


import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Date;
import java.lang.reflect.Proxy;

/**
 * 复习动态代理,便于理解调用dubbo_provider服务实例的接口的原理
 */
public class DynamicProxyTest {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException {
        // 设置变量可以保存动态代理类，默认名称以 $Proxy0 格式命名
        // System.getProperties().setProperty("sun.misc.ProxayGenerator.saveGeneratedFiles", "true");
        // 1. 创建被代理的对象，UserService接口的实现类
        DemoService demoServiceImpl = new DemoServiceImpl();
        // 2. 获取对应的 ClassLoader
        ClassLoader classLoader = demoServiceImpl.getClass().getClassLoader();
        // 3. 获取所有接口的Class，这里的demoServiceImpl只实现了一个接口DemoService，
        Class[] interfaces = demoServiceImpl.getClass().getInterfaces();
        // 4. 创建一个将传给代理类的调用请求处理器，处理所有的代理对象上的方法调用
        //    这里创建的是一个自定义的日志处理器，须传入实际的执行对象 demoServiceImpl
        InvocationHandler logHandler = new LogHandler(demoServiceImpl);
        /*
		   5.根据上面提供的信息，创建代理对象 在这个过程中，
               a.JDK会通过根据传入的参数信息动态地在内存中创建和.class 文件等同的字节码
               b.然后根据相应的字节码转换成对应的class，
               c.然后调用newInstance()创建代理实例
		 */
        DemoService proxy = (DemoService) Proxy.newProxyInstance(classLoader, interfaces, logHandler);
        // 调用代理的方法
        String msg = proxy.sayHello("proxy");
        System.out.println("proxy return: " + msg);
    }

    public static class LogHandler implements InvocationHandler {
        Object target;  // 被代理的对象，实际的方法执行者

        public LogHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            before();
            Object result = method.invoke(target, args);  // 调用 target 的 method 方法
            after();
            return result;  // 返回方法的执行结果
        }

        // 调用invoke方法之前执行
        private void before() {
            System.out.println(String.format("log start time [%s] ", new Date()));
        }

        // 调用invoke方法之后执行
        private void after() {
            System.out.println(String.format("log end time [%s] ", new Date()));
        }
    }


}
