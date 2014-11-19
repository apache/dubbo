package com.alibaba.dubbo.examples.jackson.api;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by dylan on 11/15/14.
 */
public class JacksonBean {

    private boolean bValue = true;
    private String sValue = "string";
    private int iValue = 20;
    private double dValue = 20.5;
    private float fValue = 2.18f;
    private long lValue = 3;
    private Double dValue2 = null;
    private Date date = Calendar.getInstance().getTime();
    private DateTime dateTime = DateTime.now();
    private List<JacksonInnerBean> innerBeanList = new ArrayList<JacksonInnerBean>();

    public JacksonBean(){
        innerBeanList.add(new JacksonInnerBean());
        innerBeanList.add(new JacksonInnerBean());
    }

    @Override
    public String toString() {
        return "JacksonBean{" +
                "bValue=" + bValue +
                ", sValue='" + sValue + '\'' +
                ", iValue=" + iValue +
                ", dValue=" + dValue +
                ", fValue=" + fValue +
                ", lValue=" + lValue +
                ", dValue2=" + dValue2 +
                ", date=" + date +
                ", dateTime=" + dateTime +
                ", innerBeanList=" + innerBeanList +
                '}';
    }

    public boolean isbValue() {
        return bValue;
    }

    public void setbValue(boolean bValue) {
        this.bValue = bValue;
    }

    public String getsValue() {
        return sValue;
    }

    public void setsValue(String sValue) {
        this.sValue = sValue;
    }

    public int getiValue() {
        return iValue;
    }

    public void setiValue(int iValue) {
        this.iValue = iValue;
    }

    public double getdValue() {
        return dValue;
    }

    public void setdValue(double dValue) {
        this.dValue = dValue;
    }

    public float getfValue() {
        return fValue;
    }

    public void setfValue(float fValue) {
        this.fValue = fValue;
    }

    public long getlValue() {
        return lValue;
    }

    public void setlValue(long lValue) {
        this.lValue = lValue;
    }

    public Double getdValue2() {
        return dValue2;
    }

    public void setdValue2(Double dValue2) {
        this.dValue2 = dValue2;
    }

    public List<JacksonInnerBean> getInnerBeanList() {
        return innerBeanList;
    }

    public void setInnerBeanList(List<JacksonInnerBean> innerBeanList) {
        this.innerBeanList = innerBeanList;
    }
}
