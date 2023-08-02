package org.apache.dubbo.common.utils.json;

//public record Range(Integer left, Integer right) {
//    public Integer sum() {
//        return left + right;
//    }
//}

public class Range {
    private Integer left;
    private Integer right;

    public Range(Integer left, Integer right) {
        this.left = left;
        this.right =right;
    }
    public Integer sum() {
        return left + right;
    }
}
