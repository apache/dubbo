package org.apache.dubbo.common.vo;

import java.util.Objects;

public class UserVo {
    private String name;
    private String addr;
    private int age;

    public UserVo(String name, String addr, int age) {
        this.name = name;
        this.addr = addr;
        this.age = age;
    }

    public UserVo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public static UserVo getInstance() {
        return new UserVo("dubbo", "hangzhou", 10);
    }

    @Override
    public String toString() {
        return "UserVo{" +
            "name='" + name + '\'' +
            ", addr='" + addr + '\'' +
            ", age=" + age +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserVo userVo = (UserVo) o;
        return age == userVo.age && Objects.equals(name, userVo.name) && Objects.equals(addr, userVo.addr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, addr, age);
    }
}
