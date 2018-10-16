package com.alibaba.dubbo.common;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since 2.6.5
 * 2018/10/16
 */
public class LockSwitch {

    public static AtomicInteger INIT_TASK_NUM = new AtomicInteger(0);
}
