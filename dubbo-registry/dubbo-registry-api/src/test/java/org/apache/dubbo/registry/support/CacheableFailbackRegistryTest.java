package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;

import java.lang.reflect.Method;

import org.apache.dubbo.registry.NotifyListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.URLStrParser.ENCODED_AND_MARK;
import static org.apache.dubbo.common.URLStrParser.ENCODED_QUESTION_MARK;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CacheableFailbackRegistryTest {

    private CacheableFailbackRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StubCacheableFailbackRegistry(URL.valueOf("mock://127.0.0.1"));
    }

    @Test
    void shouldKeepRemoteTimestampIntact() throws Exception {
        String base = "dubbo%3A%2F%2F127.0.0.1%3A20880%2Fdemo.Service";
        String rawProvider = base + ENCODED_QUESTION_MARK + "remote.timestamp%3D1" + ENCODED_AND_MARK + "revision%3Dabc";
        assertEquals(rawProvider, strip(rawProvider));
    }

    @Test
    void shouldRemoveOnlyTimestampWhenRemoteTimestampExists() throws Exception {
        String base = "dubbo%3A%2F%2F127.0.0.1%3A20880%2Fdemo.Service";
        String rawProvider = base + ENCODED_QUESTION_MARK
                + "timestamp%3D1" + ENCODED_AND_MARK
                + "remote.timestamp%3D2" + ENCODED_AND_MARK
                + "revision%3D3";
        String expected = base + ENCODED_QUESTION_MARK + "remote.timestamp%3D2" + ENCODED_AND_MARK + "revision%3D3";
        assertEquals(expected, strip(rawProvider));
    }

    @Test
    void shouldHandleDecodedParameters() throws Exception {
        String base = "dubbo://127.0.0.1:20880/demo.Service";
        String rawProvider = base + "?timestamp=1&remote.timestamp=2&revision=3";
        String expected = base + "?remote.timestamp=2&revision=3";
        assertEquals(expected, strip(rawProvider));
    }

    private String strip(String rawProvider) throws Exception {
        Method method = CacheableFailbackRegistry.class.getDeclaredMethod("stripOffVariableKeys", String.class);
        method.setAccessible(true);
        return (String) method.invoke(registry, rawProvider);
    }

    private static final class StubCacheableFailbackRegistry extends CacheableFailbackRegistry {
        StubCacheableFailbackRegistry(URL url) {
            super(url);
        }

	    @Override public void doRegister(URL url) {

	    }

	    @Override public void doUnregister(URL url) {

	    }

	    @Override public void doSubscribe(URL url, NotifyListener listener) {

	    }

	    @Override
        protected boolean isMatch(URL subscribeUrl, URL providerUrl) {
            return true;
        }

	    @Override public boolean isAvailable() {
		    return false;
	    }
    }
}
