package com.alibaba.com.caucho.hessian.io.beans;

import java.io.Serializable;
import java.util.List;

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PersonType that = (PersonType) o;

        if (age != that.age) return false;
        if (Double.compare(that.money, money) != 0) return false;
        if (p1 != that.p1) return false;
        if (p2 != that.p2) return false;
        if (!name.equals(that.name)) return false;
        if (!p3.equals(that.p3)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name.hashCode();
        result = 31 * result + age;
        temp = Double.doubleToLongBits(money);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) p1;
        result = 31 * result + (int) p2;
        result = 31 * result + p3.hashCode();
        return result;
    }
}
