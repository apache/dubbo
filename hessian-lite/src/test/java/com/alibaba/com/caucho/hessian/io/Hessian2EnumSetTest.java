package com.alibaba.com.caucho.hessian.io;

import com.alibaba.com.caucho.hessian.io.base.SerializeTestBase;
import com.alibaba.com.caucho.hessian.io.beans.Type;

import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class Hessian2EnumSetTest extends SerializeTestBase {

    @Test
    public void singleton() throws Exception {
        EnumSet h = EnumSet.of(Type.High);
        EnumSet set = baseHession2Serialize(h);
        assertTrue(Arrays.asList(set.toArray()).contains(Type.High));
        assertFalse(Arrays.asList(set.toArray()).contains(Type.Lower));
    }

    @Test
    public void set() throws Exception {
        EnumSet<Type> types = EnumSet.of(Type.High, Type.Lower);
        EnumSet set = baseHession2Serialize(types);
        assertTrue(set.contains(Type.High));
        assertFalse(set.contains(Type.Normal));
    }

    @Test
    public void none() throws Exception {
        EnumSet<Type> types = EnumSet.noneOf(Type.class);
        EnumSet set = baseHession2Serialize(types);
        TestCase.assertEquals(set, EnumSet.noneOf(Type.class));
    }
}
