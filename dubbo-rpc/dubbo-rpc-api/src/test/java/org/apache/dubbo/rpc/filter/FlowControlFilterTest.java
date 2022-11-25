package org.apache.dubbo.rpc.filter;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FlowControlFilterTest {
    private FlowControlFilter flowControlFilter = new FlowControlFilter();

    @BeforeEach
    public void setForTest(){
        ApplicationModel applicationModel = ApplicationModel.defaultModel();
        flowControlFilter.setApplicationModel(applicationModel);
    }

    @Test
    public void testStaticFlowControl(){
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("result"));
        when(invoker.getUrl()).thenReturn(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&flowControl=staticFlowControl"));

        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testStaticFlowControl");

        Result result = flowControlFilter.invoke(invoker, invocation);
        Assertions.assertEquals("result", result.getValue());
    }

    @Test
    public void testDynamicFlowControl(){
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("result"));
        when(invoker.getUrl()).thenReturn(URL.valueOf("test://test:11/test?accesslog=true&group=dubbo&version=1.1&flowControl=heuristicSmoothingFlowControl"));

        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testDynamicFlowControl");

        Result result = flowControlFilter.invoke(invoker, invocation);
        Assertions.assertEquals("result", result.getValue());
    }

    @Test
    public void testMultiDynamicFlowControl(){
        Invocation invocation = Mockito.mock(Invocation.class);
        when(invocation.getMethodName()).thenReturn("testMultiDynamicFlowControl");

        String urls[] = {"test1://127.0.0.1:101/DemoService?flowControl=heuristicSmoothingFlowControl","test1://127.0.0.1:102/DemoService?flowControl=heuristicSmoothingFlowControl","test1://127.0.0.1:103/DemoService?flowControl=heuristicSmoothingFlowControl"};
        for (int i = 0; i < 6; i ++){
            Invoker invoker = mock(Invoker.class);
            when(invoker.invoke(any(Invocation.class))).thenReturn(new AppResponse("result" + i));
            when(invoker.getUrl()).thenReturn(URL.valueOf(urls[i%3]));
            Result result = flowControlFilter.invoke(invoker,invocation);
            Assertions.assertEquals("result" + i,result.getValue());
        }








    }




}
