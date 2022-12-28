package org.apache.dubbo.rpc.protocol.rest.util;


import java.lang.reflect.Method;


public class MultiValueCreator {
    private final static String SPRING_MultiValueMap = "org.springframework.util.LinkedMultiValueMap";
    private final static String JAVAX_MultiValueMap = "org.jboss.resteasy.specimpl.MultivaluedMapImpl";

    private static Class multiValueMapClass = null;
    private static Method multiValueMapAdd = null;
    private static Method multiValueMapConstruct = null;

    static {
        multiValueMapClass = ReflectUtils.findClassTryException(SPRING_MultiValueMap, JAVAX_MultiValueMap);
        multiValueMapAdd = ReflectUtils.getMethodAndTryCatch(multiValueMapClass, "add", new Class[]{String.class, String.class});
    }


    public static Object createMultiValueMap() {
        try {
            return multiValueMapClass.newInstance();
        } catch (Exception e) {

        }

        return null;
    }

    public static void add(Object multiValueMap, String key, String value) {
        try {
            ReflectUtils.invokeAndTryCatch(multiValueMap, multiValueMapAdd, new String[]{key, value});
        } catch (Exception e) {

        }
    }


}
