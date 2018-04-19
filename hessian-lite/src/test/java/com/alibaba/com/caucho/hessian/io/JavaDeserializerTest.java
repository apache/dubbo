package com.alibaba.com.caucho.hessian.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;

import org.junit.Test;

import com.alibaba.com.caucho.hessian.io.base.SerializeTestBase;
import com.alibaba.com.caucho.hessian.io.beans.ConstructNPE;


public class JavaDeserializerTest extends SerializeTestBase {

    /**
     * <a href="https://github.com/apache/incubator-dubbo/issues/210">#210</a>
     * @see org.springframework.jdbc.UncategorizedSQLException
     * @see org.springframework.beans.PropertyAccessException
     */
    @Test
    public void testConstructorNPE() throws Exception {
        String sql = "select * from demo";
        SQLException sqlEx = new SQLException("just a sql exception");
        PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(new Object(), "name", "old", "new");
        ConstructNPE normalNPE = new ConstructNPE("junit", sql, sqlEx, propertyChangeEvent);

        for (int repeat = 0; repeat < 2; repeat++) {
            assertDesEquals(normalNPE, baseHession2Serialize(normalNPE));
            assertCompatibleConstructNPE(factory.getDeserializer(normalNPE.getClass()), true);
        }
    }

    private void assertDesEquals(ConstructNPE expected, ConstructNPE actual) {
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getCause().getClass(), actual.getCause().getClass());
        assertEquals(expected.getSql(), actual.getSql());
    }

    private void assertCompatibleConstructNPE(Deserializer deserializer, boolean compatible) throws Exception {
        assertEquals(JavaDeserializer.class, deserializer.getClass());
        assertEquals(compatible, getFieldValue(deserializer, "compatibleConstructNPE"));
        if (compatible) assertNotNull(getFieldValue(deserializer, "compatibleConstructor"));
    }
}
