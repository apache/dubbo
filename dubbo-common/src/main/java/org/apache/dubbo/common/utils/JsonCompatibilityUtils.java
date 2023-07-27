package org.apache.dubbo.common.utils;

import java.lang.reflect.*;
import java.util.*;

public class JsonCompatibilityUtils {

    private Set<String> unsupportedClasses = new HashSet<>(Arrays.asList("byte", "Byte"));

    /**
     * Determine whether a Class can be serialized by JSON.
     * @param clazz Incoming Class.
     * @return If a Class can be serialized by JSON, return true;
     * else return false.
     */
    public boolean checkJsonCompatibility(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();

        boolean result;

        for (Method method : methods) {
            Type[] types = method.getGenericParameterTypes();
            List<Type> typeList = new ArrayList<>(Arrays.asList(types));
            Type returnType =  method.getGenericReturnType();
            typeList.add(returnType);
            for (Type type : typeList) {
                result = checkClass(type);
                if (!result) {
                    System.out.printf("%s Not support !%n", clazz.getName());
                    return false;
                }
                System.out.println("Parameter Type: " + type.getTypeName());
            }
        }
        return true;
    }


    /**
     * Determine whether a Type can be serialized by JSON.
     * @param classType Incoming Type.
     * @return If a Type can be serialized by JSON, return true;
     * else return false.
     */
    private boolean checkClass(Type classType) {

        boolean result;

        if (classType instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) classType).getActualTypeArguments();
            classType = ((ParameterizedType) classType).getRawType();
            for (Type type : types) {
                result = checkClass(type);
                if (!result) {
                    return false;
                }
            }
        } else if (classType instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) classType).getGenericComponentType();
            result = checkClass(componentType);
            if (!result) {
                return false;
            }
        } else if (classType instanceof Class<?>) {
            Class<?> clazz = (Class<?>) classType;

            String className = clazz.getName();
            System.out.println("====::>" + className);

            if (clazz.isArray()) {
                Type componentType = clazz.getComponentType();
                result = checkClass(componentType);
                if (!result) {
                    return false;
                }
            } else if (clazz.isPrimitive()) {
                // deal with case of basic byte
                if (this.unsupportedClasses.contains(className)) {
                    return false;
                }
            } else if (className.startsWith("java") || className.startsWith("javax")) {
                if (this.unsupportedClasses.contains(className)) {
                    return false;
                }
            } else {
                // deal with case of interface
                if (clazz.isInterface()) {
                    return false;
                }
                // deal with field one by one
                for (Field field : clazz.getDeclaredFields()) {
                    Type type = field.getGenericType();
                    Class<?> fieldClass = field.getType();
                    result = checkClass(type);
                    if (!result) {
                        return false;
                    }
                }
            }
        }

        return true;

        // if (Modifier.isAbstract(clazz.getModifiers())) {
        //     return false;
        // }
        // Type type = clazz.getGenericSuperclass();
        // if (type instanceof ParameterizedType) {
        //     return false;
        // }
        // return true;
    }

}
