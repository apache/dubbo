package org.apache.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.remoting.exchange.ResponseCallback;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.protocol.dubbo.FutureAdapter;
import org.apache.dubbo.rpc.AppResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author zhenyu.nie created on 2020 2020/5/9 16:23
 */
public class FutureAdapterTest {

    @Test
    public void testCallbackResponseInstanceOfOldResult() {
        AppResponse response = new AppResponse("response");
        FutureAdapter<Object> futureAdapter = new FutureAdapter<>(CompletableFuture.completedFuture(response));
        futureAdapter.getFuture().setCallback(new ResponseCallback() {
            @Override
            public void done(Object response) {
                assertThat(response, instanceOf(Result.class));
            }

            @Override
            public void caught(Throwable exception) {

            }
        });
    }
}
