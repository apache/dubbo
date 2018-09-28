package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.TypeDescriptor;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author cvictory ON 2018/9/20
 */
public class MapTypeBuilderTest {

    private MapTypeBuilder mapTypeBuilder = new MapTypeBuilder();

    @Test
    public void testAcceptWhenMap() {
        String[] param = new String[2];
        Class c = param.getClass();
        Assert.assertFalse(mapTypeBuilder.accept(null, c));
    }

    @Test
    public void testAcceptWhenNull() {
        Assert.assertFalse(mapTypeBuilder.accept(null, null));
    }

    @Test
    public void testAcceptWhenNotMap() {
        Assert.assertFalse(mapTypeBuilder.accept(null, ArrayList.class));
    }

    @Test
    public void testBuildWhenSimpleMap() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteSimpleMap", Map.class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        TypeDescriptor td = mapTypeBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "java.util.Map<java.lang.String, java.lang.Integer>");

        Method readMethod = targetClass.getMethod("testReadSimpleMap", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = mapTypeBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "java.util.Map<java.lang.String, java.lang.Integer>");

        Assert.assertTrue(cache.isEmpty());
    }

    @Test
    public void testBuildWhenComplexMap() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteComplexMap", Map.class, Map.class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        TypeDescriptor td1 = mapTypeBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td1);
        Assert.assertNotNull(td1);
        Assert.assertEquals(td1.getType(), "java.util.Map<java.lang.String, java.lang.String>");

        TypeDescriptor td2 = mapTypeBuilder.build(genericParamTypes[1], paramTypes[1], cache);
        System.out.println(td2);
        Assert.assertNotNull(td2);
        Assert.assertEquals(td2.getType(), "java.util.Map<java.lang.String, org.apache.dubbo.servicedata.metadata.builder.ComplexObject>");

        Method readMethod = targetClass.getMethod("testReadComplexMap", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = mapTypeBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "java.util.Map<java.lang.String, org.apache.dubbo.servicedata.metadata.builder.ComplexObject>");

        Assert.assertTrue(cache.size() == 2);
        Assert.assertTrue(cache.get(ComplexObject.class) != null);
        Assert.assertEquals(cache.get(ComplexObject.class).getType(), "org.apache.dubbo.servicedata.metadata.builder.ComplexObject");
    }
}
