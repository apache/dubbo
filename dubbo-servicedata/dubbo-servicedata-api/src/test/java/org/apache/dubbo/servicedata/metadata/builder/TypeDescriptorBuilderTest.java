package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.TypeDescriptor;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *  2018/9/29
 */
public class TypeDescriptorBuilderTest {


    @Test
    public void testStaticBuildWhenSimpleArray() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteSimpleArray", String[].class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        TypeDescriptor td = TypeDescriptorBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "java.lang.String[]");

        Method readMethod = targetClass.getMethod("testReadSimpleArray", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = TypeDescriptorBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "java.lang.String[]");

        Assert.assertTrue(cache.isEmpty());
    }

    @Test
    public void testStaticBuildWhenComplextObject() throws NoSuchMethodException {

        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteComplexObject", ComplexObject.class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();


        TypeDescriptor td = TypeDescriptorBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "org.apache.dubbo.servicedata.metadata.builder.ComplexObject");
        Assert.assertTrue(td.isCustom());
        Assert.assertTrue(td.getProperties().entrySet().size() >= 4);

        Assert.assertTrue(cache.size() == 2);
        Assert.assertEquals(td, cache.get(ComplexObject.class));
        Assert.assertNotNull(cache.get(ComplexObject.ComplexInnerObject.class));
    }

    /**
     * test builder and getTypeDescriptorMap method.
     *
     * @throws NoSuchMethodException
     */
    @Test
    public void testBuildWhenComplextObject() throws NoSuchMethodException {
        TypeDescriptorBuilder typeDescriptorBuilder = new TypeDescriptorBuilder();

        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteComplexObject", ComplexObject.class);
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();


        TypeDescriptor td = typeDescriptorBuilder.build(genericParamTypes[0], paramTypes[0]);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "org.apache.dubbo.servicedata.metadata.builder.ComplexObject");
        Assert.assertTrue(td.isCustom());
        Assert.assertTrue(td.getProperties().entrySet().size() >= 4);

        Assert.assertTrue(typeDescriptorBuilder.getTypeDescriptorMap().size() == 2);
        Assert.assertEquals(td, typeDescriptorBuilder.getTypeDescriptorMap().get(ComplexObject.class.getName()));
        Assert.assertNotNull(typeDescriptorBuilder.getTypeDescriptorMap().get(ComplexObject.ComplexInnerObject.class.getName()));
    }

    @Test
    public void testGetGenericTypeBuilder() {
        Assert.assertNull(TypeDescriptorBuilder.getGenericTypeBuilder(null, ComplexObject.class));
        Assert.assertNotNull(TypeDescriptorBuilder.getGenericTypeBuilder(null, String[].class));
        Assert.assertTrue(TypeDescriptorBuilder.getGenericTypeBuilder(null, String[].class) instanceof ArrayTypeBuilder);
        Assert.assertNotNull(TypeDescriptorBuilder.getGenericTypeBuilder(null, ArrayList.class));
        Assert.assertTrue(TypeDescriptorBuilder.getGenericTypeBuilder(null, ArrayList.class) instanceof CollectionTypeBuilder);
        Assert.assertNotNull(TypeDescriptorBuilder.getGenericTypeBuilder(null, HashMap.class));
        Assert.assertTrue(TypeDescriptorBuilder.getGenericTypeBuilder(null, HashMap.class) instanceof MapTypeBuilder);
        Assert.assertNotNull(TypeDescriptorBuilder.getGenericTypeBuilder(null, SingleEnum.class));
        Assert.assertTrue(TypeDescriptorBuilder.getGenericTypeBuilder(null, SingleEnum.class) instanceof EnumTypeBuilder);
    }
}
