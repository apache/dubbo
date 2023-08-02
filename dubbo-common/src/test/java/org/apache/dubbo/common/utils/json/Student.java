package org.apache.dubbo.common.utils.json;

import java.io.Serializable;
import java.util.List;

public class Student<W> implements Serializable {
    private Integer type;

    private List<String> names;

    private List<W> namesT;

    private W age;

    private String name;

    public Student(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Student{" +
                "type=" + type +
                ", name='" + name + '\'' +
                '}';
    }
}
