package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.MethodDescriptor;
import org.apache.dubbo.servicedata.metadata.ServiceDescriptor;
import org.apache.dubbo.servicedata.metadata.TypeDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * @author cvictory ON 2018/9/18
 */
public class ServiceDescriptorBuilder {

    public static ServiceDescriptor build(final Class<?> interfaceClass) {
        ServiceDescriptor sd = new ServiceDescriptor();
        sd.setName(interfaceClass.getCanonicalName());
        sd.setCodeSource(getCodeSource(interfaceClass));

        TypeDescriptorBuilder builder = new TypeDescriptorBuilder();
        Method[] methods = interfaceClass.getMethods();
        for (Method method : methods) {
            MethodDescriptor md = new MethodDescriptor();
            md.setName(method.getName());

            // Process parameter types.
            Class<?>[] paramTypes = method.getParameterTypes();
            Type[] genericParamTypes = method.getGenericParameterTypes();

            String[] parameterTypes = new String[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                TypeDescriptor td = builder.build(genericParamTypes[i], paramTypes[i]);
                parameterTypes[i] = td.getType();
            }
            md.setParameterTypes(parameterTypes);

            // Process return type.
            TypeDescriptor td = builder.build(method.getGenericReturnType(), method.getReturnType());
            md.setReturnType(td.getType());

            sd.getMethodDescriptors().add(md);
        }

        sd.setTypes(builder.getTypeDescriptorMap());
        return sd;
    }

    static String getCodeSource(Class<?> clazz) {
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        if (protectionDomain == null || protectionDomain.getCodeSource() == null) {
            return null;
        }

        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        URL location = codeSource.getLocation();
        if (location == null) {
            return null;
        }

        String path = codeSource.getLocation().toExternalForm();

        if (path.endsWith(".jar") && path.contains("/")) {
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return path;
    }

}
