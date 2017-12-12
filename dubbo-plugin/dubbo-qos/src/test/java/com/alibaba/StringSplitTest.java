package com.alibaba;

import org.junit.Test;

/**
 * @author qinliujie
 * @date 2017/11/20
 */
public class StringSplitTest {
    @Test
    public void stringSplitTest(){
        String str = new String("?\\\\      helloworld ac");
        String[] result = str.split("(?<![\\\\]) ");
    }
}
