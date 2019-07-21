package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.annotations.GenericAlias;

import java.util.Map;

/**
 * @author hexiufeng
 * @date 2019/6/11下午7:47
 */
public class ResponseDemo {
    private int statusCode;

    @GenericAlias("_msg")
    private String msg;
    @GenericAlias("_desc")
    private String desc;


    @GenericAlias("_extraInfo")
    public Map<String, String> getExtra() {
        return extra;
    }

    private Map<String,String> extra;


    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
    }
}
