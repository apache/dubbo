package com.alibaba.dubbo.demo;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Demo for parameter validation
 */
public class RequestParam implements Serializable {

    private static final long serialVersionUID = -2985070735827125058L;

    public RequestParam() {
    }

    public RequestParam(String name) {
        this.name = name;
    }

    @NotEmpty(message = "Name required!")
    @Size(min = 6, max = 20, message = "Between 6 and 20!")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
