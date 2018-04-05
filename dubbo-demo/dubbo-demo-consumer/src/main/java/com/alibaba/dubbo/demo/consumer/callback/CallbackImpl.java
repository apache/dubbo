package com.alibaba.dubbo.demo.consumer.callback;

public class CallbackImpl {

    public void done(String result) {
        System.out.println("结果：" + result);
    }

    public void handleException(Throwable e) {
        System.out.println("异常：" + e.getMessage());
    }

}
