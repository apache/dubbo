package org.apache.dubbo.rpc.service;
import org.apache.dubbo.common.utils.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author owen.cai
 * @create_date 2022/8/1
 * @alter_author
 * @alter_date
 */
public class GenericExceptionTest {

    @Test
    void jsonSupport() throws IOException {
        {
            GenericException src = new GenericException();
            String s = JsonUtils.getJson().toJson(src);
            GenericException dst = JsonUtils.getJson().toJavaObject(s, GenericException.class);
            Assertions.assertEquals(src.getExceptionClass(), dst.getExceptionClass());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
            Assertions.assertEquals(src.getMessage(), dst.getMessage());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
        }
        {
            GenericException src = new GenericException(this.getClass().getSimpleName(), "test");
            String s = JsonUtils.getJson().toJson(src);
            GenericException dst = JsonUtils.getJson().toJavaObject(s, GenericException.class);
            Assertions.assertEquals(src.getExceptionClass(), dst.getExceptionClass());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
            Assertions.assertEquals(src.getMessage(), dst.getMessage());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
        }
        {
            Throwable throwable = new Throwable("throwable");
            GenericException src = new GenericException(throwable);
            String s = JsonUtils.getJson().toJson(src);
            GenericException dst = JsonUtils.getJson().toJavaObject(s, GenericException.class);
            Assertions.assertEquals(src.getExceptionClass(), dst.getExceptionClass());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
            Assertions.assertEquals(src.getMessage(), dst.getMessage());
            Assertions.assertEquals(src.getExceptionMessage(), dst.getExceptionMessage());
        }
    }
}
