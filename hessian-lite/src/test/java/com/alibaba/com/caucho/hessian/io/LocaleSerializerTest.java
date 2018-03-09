package com.alibaba.com.caucho.hessian.io;

import java.io.IOException;
import java.util.Locale;

import org.junit.Test;

import com.alibaba.com.caucho.hessian.io.base.SerializeTestBase;

import junit.framework.TestCase;

public class LocaleSerializerTest extends SerializeTestBase {

    /** {@linkplain LocaleSerializer#writeObject(Object, AbstractHessianOutput)} */
    @Test
    public void locale() throws IOException {
        assertLocale(null);
        assertLocale(new Locale(""));
        assertLocale(new Locale("zh"));
        assertLocale(new Locale("zh", "CN"));
        assertLocale(new Locale("zh-hant", "CN"));
        assertLocale(new Locale("zh-hant", "CN", "GBK"));
    }

    private void assertLocale(Locale locale) throws IOException {
        TestCase.assertEquals(locale, baseHession2Serialize(locale));
        TestCase.assertEquals(locale, baseHessionSerialize(locale));
    }
}