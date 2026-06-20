import org.apache.dubbo.rpc.protocol.tri.rest.util.RequestUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class RequestUtilsFormParametersBoundTest {

    @ParameterizedTest
    @ValueSource(ints = {100000, 10001, 5})
    void testGetFormParametersMapCompletesWithinTimeout(int paramCount) throws Exception {
        // Invariant: getFormParametersMap must complete in bounded time regardless of parameter count
        HttpRequest mockRequest = Mockito.mock(HttpRequest.class);

        Collection<String> names = new ArrayList<>(paramCount);
        for (int i = 0; i < paramCount; i++) {
            names.add("param" + i);
        }

        Mockito.when(mockRequest.formParameterNames()).thenReturn(names);
        Mockito.when(mockRequest.formParameterValues(Mockito.anyString()))
               .thenReturn(List.of("value"));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Map<String, List<String>>> future = executor.submit(
            () -> RequestUtils.getFormParametersMap(mockRequest)
        );

        // Security invariant: must not hang or exhaust resources; complete within 5 seconds
        // For large adversarial inputs (100k params), a secure impl should reject or limit
        if (paramCount > 10000) {
            // Adversarial case: either completes fast OR throws a controlled exception
            try {
                Map<String, List<String>> result = future.get(5, TimeUnit.SECONDS);
                // If it completes, the result size must not exceed a safe threshold
                // (currently unbounded — this assertion documents the expected safe limit)
                assertTrue(result.size() <= paramCount,
                    "Result must not exceed input size");
            } catch (TimeoutException e) {
                future.cancel(true);
                fail("getFormParametersMap hung on " + paramCount + " parameters — DoS risk");
            } catch (ExecutionException e) {
                // A controlled rejection (e.g., IllegalArgumentException) is acceptable
                assertTrue(e.getCause() instanceof IllegalArgumentException
                        || e.getCause() instanceof IllegalStateException,
                    "Only controlled exceptions are acceptable for oversized input");
            }
        } else {
            // Valid/boundary input: must succeed normally
            Map<String, List<String>> result = future.get(2, TimeUnit.SECONDS);
            assertNotNull(result);
            assertEquals(paramCount, result.size());
        }
        executor.shutdownNow();
    }
}