package org.apache.dubbo.config.spring6.utils;

import java.io.Serializable;

/**
 * @author: crazyhzm@apache.org
 */
public class Person implements Serializable {

    private String name;

    public Person(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
