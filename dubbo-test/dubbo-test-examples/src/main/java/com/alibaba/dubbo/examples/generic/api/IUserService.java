package com.alibaba.dubbo.examples.generic.api;

import com.alibaba.dubbo.examples.generic.api.IUserService.Params;
import com.alibaba.dubbo.examples.generic.api.IUserService.User;

import java.io.Serializable;

public interface IUserService extends IService<Params, User> {


    public static class Params implements Serializable {
        private static final long serialVersionUID = 1L;

        public Params(String query) {
        }
    }

    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private String name;
        public User(int id, String name) {
            super();
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "User [id=" + id + ", name=" + name + "]";
        }
    }
}
