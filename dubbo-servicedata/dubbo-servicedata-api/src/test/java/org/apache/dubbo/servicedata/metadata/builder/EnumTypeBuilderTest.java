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
public class EnumTypeBuilderTest {

    private EnumTypeBuilder enumTypeBuilder = new EnumTypeBuilder();

    @Test
    public void testAcceptWhenEnum() {
        Class c = SingleEnum.class;
        Assert.assertTrue(enumTypeBuilder.accept(null, c));
    }

    @Test
    public void testAcceptWhenNull() {
        Assert.assertFalse(enumTypeBuilder.accept(null, null));
    }

    @Test
    public void testAcceptWhenNotEnum() {
        Assert.assertFalse(enumTypeBuilder.accept(null, ArrayList.class));
    }

    @Test
    public void testBuildWhenSingleEnum() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteSingleEnum", SingleEnum.class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();

        TypeDescriptor td = enumTypeBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "org.apache.dubbo.servicedata.metadata.builder.SingleEnum");

        Method readMethod = targetClass.getMethod("testReadSingleEnum", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = enumTypeBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "org.apache.dubbo.servicedata.metadata.builder.SingleEnum");

        Assert.assertFalse(cache.isEmpty());
    }

    @Test
    public void testBuildWhenComplexEnum() throws NoSuchMethodException {
        Class targetClass = TestService.class;
        Method method = targetClass.getMethod("testWriteComplexEnum", ComplexEnum.class);
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        Class<?>[] paramTypes = method.getParameterTypes();
        Type[] genericParamTypes = method.getGenericParameterTypes();


        TypeDescriptor td2 = enumTypeBuilder.build(genericParamTypes[0], paramTypes[0], cache);
        System.out.println(td2);
        Assert.assertNotNull(td2);
        Assert.assertEquals(td2.getType(), "org.apache.dubbo.servicedata.metadata.builder.ComplexEnum");

        Method readMethod = targetClass.getMethod("testReadComplexEnum", int.class);
        Class returnType = readMethod.getReturnType();
        Type genericReturnType = readMethod.getGenericReturnType();
        TypeDescriptor rTd = enumTypeBuilder.build(genericReturnType, returnType, cache);
        Assert.assertNotNull(rTd);
        Assert.assertEquals(rTd.getType(), "org.apache.dubbo.servicedata.metadata.builder.ComplexEnum");

        Assert.assertTrue(cache.size() == 1);
        Assert.assertTrue(cache.get(ComplexEnum.class) != null);
        Assert.assertEquals(cache.get(ComplexEnum.class).getType(), "org.apache.dubbo.servicedata.metadata.builder.ComplexEnum");
    }
}
