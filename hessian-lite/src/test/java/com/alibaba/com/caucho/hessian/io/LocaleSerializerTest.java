package com.alibaba.com.caucho.hessian.io;

import com.alibaba.com.caucho.hessian.io.base.SerializeTestBase;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Locale;

public class LocaleSerializerTest extends SerializeTestBase {
    @Test
    public void hessian2() throws Exception {
        Locale locale = new Locale("zh");
        Locale result = baseHession2Serialize(locale);
        TestCase.assertEquals(locale, result);
        locale = new Locale("zh", "CN");
        result = baseHession2Serialize(locale);
        TestCase.assertEquals(locale, result);
        locale = new Locale("zh", "CN", "GBK");
        result = baseHession2Serialize(locale);
        TestCase.assertEquals(locale, result);
        locale = new Locale("zh-hant", "CN");
        result = baseHession2Serialize(locale);
        TestCase.assertEquals(locale, result);
    }

    @Test
    public void hessian1() throws Exception {
        Locale locale = new Locale("zh");
        Locale result = baseHessionSerialize(locale);
        TestCase.assertEquals(locale, result);
        locale = new Locale("zh", "CN");
        result = baseHessionSerialize(locale);
        TestCase.assertEquals(locale, result);
        locale = new Locale("zh", "CN", "GBK");
        result = baseHessionSerialize(locale);
        TestCase.assertEquals(locale, result);
        locale = new Locale("zh-hant", "CN");
        result = baseHessionSerialize(locale);
        TestCase.assertEquals(locale, result);
    }
}
