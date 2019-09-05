package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.annotations.GenericFeature;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author hexiufeng
 * @date 2019/6/11下午7:47
 */
public class ResponseDemo {
    private int statusCode;

    @GenericFeature(alias = "_msg")
    private String msg;
    @GenericFeature(alias = "_desc")
    private String desc;

    @GenericFeature(alias = "lValue",longAsString = true)
    private long lValue;

    @GenericFeature(alias = "ignore", ignore = true)
    private String ignore = "ignore";

    @GenericFeature(alias = "dateTime", dateFormatter = "yyyy-MM-dd hh:mm:ss")
    private LocalDateTime dateTime = LocalDateTime.now();


    @GenericFeature(alias = "_extraInfo")
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

    public long getlValue() {
        return lValue;
    }

    public void setlValue(long lValue) {
        this.lValue = lValue;
    }
}
