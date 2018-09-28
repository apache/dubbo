package org.apache.dubbo.servicedata.metadata.builder;

import org.apache.dubbo.servicedata.metadata.TypeDescriptor;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author cvictory ON 2018/9/28
 */
public class DefaultTypeBuilderTest {

    @Test
    public void testWhenBigDecimal() {
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        TypeDescriptor td = DefaultTypeBuilder.build(BigDecimal.class, cache);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "java.math.BigDecimal");
        Assert.assertFalse(td.isCustom());

        Assert.assertTrue(cache.isEmpty());
    }

    @Test
    public void testWhenString() {
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        TypeDescriptor td = DefaultTypeBuilder.build(String.class, cache);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "java.lang.String");
        Assert.assertFalse(td.isCustom());

        Assert.assertTrue(cache.isEmpty());
    }

    @Test
    public void testWhenInt() {
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        TypeDescriptor td = DefaultTypeBuilder.build(int.class, cache);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "int");
        Assert.assertFalse(td.isCustom());

        Assert.assertTrue(cache.isEmpty());
    }

    @Test
    public void testWhenComplextObject() {
        Map<Class<?>, TypeDescriptor> cache = new HashMap<Class<?>, TypeDescriptor>();
        TypeDescriptor td = DefaultTypeBuilder.build(ComplexObject.class, cache);
        Assert.assertNotNull(td);
        Assert.assertEquals(td.getType(), "org.apache.dubbo.servicedata.metadata.builder.ComplexObject");
        Assert.assertTrue(td.isCustom());
        Assert.assertTrue(td.getProperties().entrySet().size() >= 4);

        Assert.assertTrue(cache.size() == 2);
        Assert.assertEquals(td, cache.get(ComplexObject.class));
        Assert.assertNotNull(cache.get(ComplexObject.ComplexInnerObject.class));
    }


}
