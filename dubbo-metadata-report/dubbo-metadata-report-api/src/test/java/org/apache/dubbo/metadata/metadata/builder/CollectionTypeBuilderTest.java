package org.apache.dubbo.metadata.metadata.builder;

import org.apache.dubbo.metadata.metadata.TypeDescriptor;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  2018/9/20
 */
public class CollectionTypeBuilderTest {

    private CollectionTypeBuilder collectionTypeBuilder = new CollectionTypeBuilder();

    @Test
    public void testAcceptWhenNotCollection() {
        String[] param = new String[2];
        Class c = param.getClass();
        Assert.assertFalse(collectionTypeBuilder.accept(null, c));
    }

    @Test
    public void testAcceptWhenNull() {
        Assert.assertFalse(collectionTypeBuilder.accept(null, null));
    }

    @Test
    public void testAcceptWhenCollection() {
        Assert.assertTrue(collectionTypeBuilder.accept(null, ArrayList.class));
    }

    @Test
    public void testBuildWhenSimpleList() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteSimpleCollection", List.class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        TypeDescriptor td = collectionTypeBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "java.util.List<java.lang.String>");

        Method readMethod = targetClass.getMethod("testReadSimpleCollection", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = collectionTypeBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "java.util.List<java.lang.Integer>");

        Assert.assertTrue(cache.isEmpty());
    }

    @Test
    public void testBuildWhenComplexList() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteComplexCollection", List.class, List.class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        TypeDescriptor td1 = collectionTypeBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td1);
        Assert.assertNotNull(td1);
        Assert.assertEquals(td1.getType(), "java.util.List<java.lang.Long>");

        TypeDescriptor td2 = collectionTypeBuilder.build(genericParamTypes[1], paramTypes[1], cache);
        System.out.println(td2);
        Assert.assertNotNull(td2);
        Assert.assertEquals(td2.getType(), "java.util.List<org.apache.dubbo.metadata.metadata.builder.ComplexObject>");

        Method readMethod = targetClass.getMethod("testReadComplexCollection", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = collectionTypeBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "java.util.Set<org.apache.dubbo.metadata.metadata.builder.ComplexObject>");

        Assert.assertTrue(cache.size() == 2);
        Assert.assertTrue(cache.get(ComplexObject.class) != null);
        Assert.assertEquals(cache.get(ComplexObject.class).getType(), "org.apache.dubbo.metadata.metadata.builder.ComplexObject");
    }
}
