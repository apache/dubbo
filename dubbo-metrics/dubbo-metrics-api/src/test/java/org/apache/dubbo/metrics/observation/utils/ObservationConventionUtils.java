package org.apache.dubbo.metrics.observation.utils;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.mockito.Mockito;

import java.lang.reflect.Field;

public class ObservationConventionUtils {


    public static Invoker<?> getMockInvokerWithUrl(){
        URL url = URL.valueOf("dubbo://127.0.0.1:12345/com.example.TestService?anyhost=true&application=test&category=providers&dubbo=2.0.2&generic=false&interface=com.example.TestService&methods=testMethod&pid=26716&side=provider&timestamp=1633863896653");
        Invoker<?> invoker = Mockito.mock(Invoker.class);
        Mockito.when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

    public static String getValueForKey(KeyValues keyValues, Object key) throws NoSuchFieldException, IllegalAccessException {
        Field f =  KeyValues.class.getDeclaredField("keyValues");
        f.setAccessible(true);
        KeyValue[] kv = (KeyValue[]) f.get(keyValues);
        for (KeyValue keyValue : kv) {
            if (keyValue.getKey().equals(key)) {
                return keyValue.getValue();
            }
        }
        return null;
    }
}
