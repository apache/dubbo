package org.apache.dubbo.config.security;

import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.common.utils.SerializeClassChecker;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class SecurityManager {
    private final Set<String> allowedPrefix = new LinkedHashSet<>();

    private final SerializeClassChecker checker = SerializeClassChecker.getInstance();

    private final Set<AllowClassNotifyListener> listeners;

    public SecurityManager(FrameworkModel frameworkModel) {
        listeners = frameworkModel.getExtensionLoader(AllowClassNotifyListener.class).getSupportedExtensionInstances();
    }

    public void registerInterface(Class<?> clazz) {
        Set<Class<?>> markedClass = new HashSet<>();
        markedClass.add(clazz);

        addToAllow(clazz.getName());

        Method[] methodsToExport = clazz.getMethods();

        for (Method method : methodsToExport) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> parameterType : parameterTypes) {
                checkClass(markedClass, parameterType);
            }

            Type[] genericParameterTypes = method.getGenericParameterTypes();
            for (Type genericParameterType : genericParameterTypes) {
                if (genericParameterType instanceof Class) {
                    checkClass(markedClass, (Class<?>) genericParameterType);
                }
            }

            Class<?> returnType = method.getReturnType();
            checkClass(markedClass, returnType);

            Type genericReturnType = method.getGenericReturnType();
            if (genericReturnType instanceof Class) {
                checkClass(markedClass, (Class<?>) genericReturnType);
            }

            Class<?>[] exceptionTypes = method.getExceptionTypes();
            for (Class<?> exceptionType : exceptionTypes) {
                checkClass(markedClass, exceptionType);
            }

            Type[] genericExceptionTypes = method.getGenericExceptionTypes();
            for (Type genericExceptionType : genericExceptionTypes) {
                if (genericExceptionType instanceof Class) {
                    checkClass(markedClass, (Class<?>) genericExceptionType);
                }
            }
        }
    }

    private void checkClass(Set<Class<?>> markedClass, Class<?> clazz) {
        if (markedClass.contains(clazz)) {
            return;
        }

        markedClass.add(clazz);

        addToAllow(clazz.getName());

        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> interfaceClass : interfaces) {
            checkClass(markedClass, interfaceClass);
        }

        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            checkClass(markedClass, superclass);
        }

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            Class<?> fieldClass = field.getDeclaringClass();
            checkClass(markedClass, fieldClass);
        }
    }

    private void addToAllow(String className) {
        if (!checker.validateClass(className, false)) {
            return;
        }

        boolean modified;

        // ignore jdk
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("com.sun.") ||
            className.startsWith("sun.") || className.startsWith("jdk.")) {
            modified = allowedPrefix.add(className);
            if (modified) {
                for (AllowClassNotifyListener listener : listeners) {
                    listener.notify(allowedPrefix);
                }
            }
            return;
        }

        // add group package
        String[] subs = className.split("\\.");
        if (subs.length > 3) {
            modified = allowedPrefix.add(subs[0] + "." + subs[1] + "." + subs[2]);
        } else {
            modified = allowedPrefix.add(className);
        }

        if (modified) {
            for (AllowClassNotifyListener listener : listeners) {
                listener.notify(allowedPrefix);
            }
        }
    }

}
