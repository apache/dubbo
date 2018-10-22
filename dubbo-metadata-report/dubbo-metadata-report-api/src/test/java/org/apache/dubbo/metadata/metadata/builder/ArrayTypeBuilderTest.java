package org.apache.dubbo.metadata.metadata.builder;

import org.apache.dubbo.metadata.metadata.TypeDescriptor;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 *  2018/9/20
 */
public class ArrayTypeBuilderTest {

    private ArrayTypeBuilder arrayTypeBuilder = new ArrayTypeBuilder();

    @Test
    public void testAcceptWhenArray() {
        String[] param = new String[2];
        Class c = param.getClass();
        Assert.assertTrue(arrayTypeBuilder.accept(null, c));
    }

    @Test
    public void testAcceptWhenNull() {
        Assert.assertFalse(arrayTypeBuilder.accept(null, null));
    }

    @Test
    public void testAcceptWhenOtherClass() {
        Assert.assertFalse(arrayTypeBuilder.accept(null, Array.class));
    }

    @Test
    public void testBuildWhenSimpleArray() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteSimpleArray", String[].class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        TypeDescriptor td = arrayTypeBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "java.lang.String[]");

        Method readMethod = targetClass.getMethod("testReadSimpleArray", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = arrayTypeBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "java.lang.String[]");

        Assert.assertTrue(cache.isEmpty());
    }

    @Test
    public void testBuildWhenComplexArray() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteComplexArray", String[].class, ComplexObject[].class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        TypeDescriptor td1 = arrayTypeBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td1);
        Assert.assertNotNull(td1);
        Assert.assertEquals(td1.getType(), "java.lang.String[]");

        TypeDescriptor td2 = arrayTypeBuilder.build(genericParamTypes[1], paramTypes[1], cache);
        System.out.println(td2);
        Assert.assertNotNull(td2);
        Assert.assertEquals(td2.getType(), "org.apache.dubbo.metadata.metadata.builder.ComplexObject[]");

        Method readMethod = targetClass.getMethod("testReadComplexArray", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = arrayTypeBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "org.apache.dubbo.metadata.metadata.builder.ComplexObject[]");

        Assert.assertTrue(cache.size() == 2);
        Assert.assertTrue(cache.get(ComplexObject.class) != null);
        Assert.assertEquals(cache.get(ComplexObject.class).getType(), "org.apache.dubbo.metadata.metadata.builder.ComplexObject");
    }
}
