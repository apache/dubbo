package org.apache.dubbo.demo;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author cvictory ON 2018/9/17
 */
public class MethodTest {

    public static void main(String[] args) {
        Method[] methods = UserService.class.getDeclaredMethods();
        for (Method method : methods) {


            // Process parameter types.
            Class<?>[] paramTypes = method.getParameterTypes();
            Type[] genericParamTypes = method.getGenericParameterTypes();

            System.out.println(method.getName() + " paramType: " + paramTypes);
            System.out.println(method.getName() + " genericParamType: " + genericParamTypes);

        }
    }
}
