package org.apache.dubbo.rpc.protocol.tri.rest.util;

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HttpRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RequestUtilsSecurityTest {

    @ParameterizedTest
    @ValueSource(ints = {1000000, 100000, 10})
    void testFormParametersMapMaintainsMemoryBounds(int paramCount) {
        // Invariant: Processing form parameters must not consume unbounded memory
        
        HttpRequest mockRequest = new HttpRequest() {
            private final Set<String> names = generateParamNames(paramCount);
            
            @Override
            public Collection<String> formParameterNames() {
                return names;
            }
            
            @Override
            public List<String> formParameterValues(String name) {
                return Collections.singletonList("value");
            }
        };
        
        long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        Map<String, List<String>> result = RequestUtils.getFormParametersMap(mockRequest);
        
        long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = afterMemory - beforeMemory;
        
        // Memory usage should be proportional to input size with reasonable constant factor
        long maxExpectedMemory = paramCount * 200L; // ~200 bytes per parameter (generous bound)
        
        assertTrue(memoryUsed < maxExpectedMemory || paramCount <= 10,
            String.format("Memory usage %d exceeded bound %d for %d parameters", 
                memoryUsed, maxExpectedMemory, paramCount));
        
        assertEquals(paramCount, result.size());
    }
    
    private Set<String> generateParamNames(int count) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < count; i++) {
            names.add("param" + i);
        }
        return names;
    }
}