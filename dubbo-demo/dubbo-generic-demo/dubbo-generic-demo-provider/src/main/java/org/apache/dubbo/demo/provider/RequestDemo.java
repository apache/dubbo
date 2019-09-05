package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.annotations.GenericFeature;

import java.util.Map;

/**
 * @author hexiufeng
 * @date 2019/6/11下午7:46
 */
public class RequestDemo {
    @GenericFeature(alias = "_name")
    private String name;
    @GenericFeature(alias = "_description", ignore = true)
    private String desc;

    @GenericFeature(alias = "_extra")
    private Map<String,String> extra;

    @GenericFeature(alias = "lValue",longAsString = true)
    private long lValue;


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

    public long getlValue() {
        return lValue;
    }

    public void setlValue(long lValue) {
        this.lValue = lValue;
    }
}
