package com.alibaba.com.caucho.hessian.io;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

public class LocaleSerializerTest {

    /** {@linkplain LocaleSerializer#writeObject(Object, AbstractHessianOutput)} */
    @Test
    public void locale() throws IOException {
        assertLocale(new Locale("zh", "CN"));
        assertLocale(new Locale("zh-hant", "CN"));
    }

    private void assertLocale(Locale loc) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(loc);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);
        assertEquals(loc, input.readObject());
    }

}
