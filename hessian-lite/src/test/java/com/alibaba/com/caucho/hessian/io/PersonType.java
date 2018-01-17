package com.alibaba.com.caucho.hessian.io;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author jason.shang
 */
public class PersonType implements Serializable {

    String name;
    int age;
    double money;
    short p1;
    byte  p2;
    List<Short> p3;

    public PersonType(String name, int age, double money, short p1, byte p2, List<Short> p3) {
        this.name = name;
        this.age = age;
        this.money = money;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PersonType type = (PersonType) o;
        return age == type.age &&
            Double.compare(type.money, money) == 0 &&
            p1 == type.p1 &&
            p2 == type.p2 &&
            Objects.equals(name, type.name) &&
            Objects.equals(p3, type.p3);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, money, p1, p2, p3);
    }
}
