package org.apache.dubbo.servicedata.metadata.builder;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;

/**
 * @author cvictory ON 2018/9/20
 */
public class ArrayTypeBuilderTest {

    private ArrayTypeBuilder arrayTypeBuilder = new ArrayTypeBuilder();

    @Test
    public void testAcceptWhenArray(){
        String[] param = new String[2];
        Class c = param.getClass();
        Assert.assertTrue(arrayTypeBuilder.accept(null, c));
    }

    @Test
    public void testAcceptWhenNull(){
        Assert.assertFalse(arrayTypeBuilder.accept(null, null));
    }

    @Test
    public void testAcceptWhenOtherClass(){
        Assert.assertFalse(arrayTypeBuilder.accept(null, Array.class));
    }

//    @Test
//    public void testBuildWhenNotArray(){
//        arrayTypeBuilder.build()
//    }
}
