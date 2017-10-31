package com.alibaba.com.caucho.hessian.io;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.EnumSet;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class Hessian2EnumSetTest {

    @Test
    public void singleton() throws Exception {
        EnumSet h = EnumSet.of(Type.High);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(h);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);
        EnumSet set = (EnumSet) input.readObject();

        assertTrue(Arrays.asList(set.toArray()).contains(Type.High));
        assertFalse(Arrays.asList(set.toArray()).contains(Type.Lower));
    }

    @Test
    public void set() throws Exception {
        EnumSet<Type> types = EnumSet.of(Type.High, Type.Lower);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(types);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);

        EnumSet set = (EnumSet) input.readObject();
        assertTrue(set.contains(Type.High));
        assertFalse(set.contains(Type.Normal));
    }

    @Test
    public void none() throws Exception {
        EnumSet<Type> types = EnumSet.noneOf(Type.class);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(types);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);

        EnumSet set = (EnumSet) input.readObject();
        TestCase.assertEquals(set, EnumSet.noneOf(Type.class));
    }
}
