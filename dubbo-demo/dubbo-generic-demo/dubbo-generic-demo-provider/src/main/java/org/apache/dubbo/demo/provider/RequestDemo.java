package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.annotations.GenericAlias;

import java.util.Map;

/**
 * @author hexiufeng
 * @date 2019/6/11下午7:46
 */
public class RequestDemo {
    @GenericAlias("_name")
    private String name;
    @GenericAlias("_description")
    private String desc;

    @GenericAlias("_extra")
    private Map<String,String> extra;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
    }
}
