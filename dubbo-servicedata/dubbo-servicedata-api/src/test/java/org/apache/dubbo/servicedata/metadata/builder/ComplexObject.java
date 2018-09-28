package org.apache.dubbo.servicedata.metadata.builder;

import java.util.List;
import java.util.Map;

/**
 * @author cvictory ON 2018/9/28
 */
public class ComplexObject {

    private static boolean test;

    private String[] strArray;
    private List<Integer> integerList;
    private short s;
    private Map<String, Long> testMap;
    private ComplexInnerObject complexInnerObject;

    public List<Integer> getIntegerList() {
        return integerList;
    }

    public void setIntegerList(List<Integer> integerList) {
        this.integerList = integerList;
    }

    public short getS() {
        return s;
    }

    public void setS(short s) {
        this.s = s;
    }
    static class ComplexInnerObject{
        private String str;
    }
}
