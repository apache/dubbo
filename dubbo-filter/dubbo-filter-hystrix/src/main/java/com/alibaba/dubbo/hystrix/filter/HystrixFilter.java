package com.alibaba.dubbo.hystrix.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.rpc.*;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by wuyu on 2016/6/13.
 */
@Activate(group = {Constants.CONSUMER, Constants.PROVIDER}, value = "hystrixFilter")
public class HystrixFilter implements Filter {

    private Logger logger = LoggerFactory.getLogger(HystrixFilter.class);


    @Override
    public Result invoke(final Invoker<?> invoker, final Invocation invocation) throws RpcException {

        final Class<?> anInterface = invoker.getInterface();
        final String anInterfaceName = anInterface.getName();
        final String methodName = invocation.getMethodName();
        final Class<?>[] parameterTypes = invocation.getParameterTypes();
        final Object[] arguments = invocation.getArguments();

        final FallBack fallBack = anInterface.getAnnotation(FallBack.class);
        HystrixCommand<Result> hystrixCommand = new HystrixCommand<Result>(HystrixCommandGroupKey.Factory.asKey(anInterfaceName)) {
            @Override
            protected Result run() throws Exception {
                return invoker.invoke(invocation);
            }

            @Override
            protected Result getFallback() {
                ApplicationContext springContext = ServiceBean.getSpringContext();
                Class<?> aClass = fallBack.value();
                String[] beanNamesForType = springContext.getBeanNamesForType(aClass);
                Object bean = null;
                Result result = null;
                if (beanNamesForType.length > 0) {
                    bean = springContext.getBean(aClass);
                } else {
                    try {
                        bean = aClass.newInstance();
                    } catch (Exception e) {
                    }
                }

                try {
                    Method method = getMethod(anInterface, methodName, parameterTypes);
                    Object invoke = method.invoke(bean, arguments);
                    result = new RpcResult(invoke);
                } catch (Exception e) {
                    super.getFallback();
                }
                return result;
            }
        };
        return hystrixCommand.execute();
    }

    public Method getMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }


}
