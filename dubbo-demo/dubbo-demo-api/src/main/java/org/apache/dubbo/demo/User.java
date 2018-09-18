package org.apache.dubbo.demo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author cvictory ON 2018/9/17
 */
public class User {
    private List<Item> items;
    Integer count;
    private BigDecimal bigDecimal;
    private Item item;

    public static class Item{
        private String name;
        private String value;
    }
}
