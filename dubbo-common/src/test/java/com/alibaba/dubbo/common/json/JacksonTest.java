package com.alibaba.dubbo.common.json;

import com.alibaba.dubbo.common.serialize.support.json.FastJsonObjectOutput;
import com.alibaba.dubbo.common.serialize.support.json.JacksonObjectInput;
import com.alibaba.dubbo.common.serialize.support.json.JacksonObjectOutput;
import com.alibaba.dubbo.common.utils.CollectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dylan on 11/13/14.
 */
public class JacksonTest {
    @Test
    public void testWrite() throws  Exception{
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        JacksonObjectOutput output = new JacksonObjectOutput(bos);
//        PrintWriter writer = new PrintWriter(new OutputStreamWriter(System.out));
        output.writeUTF("2.0.0");
        output.writeBool(true);
        output.writeByte((byte) 2);
        output.writeBytes(new String("abc").getBytes());
        output.writeDouble(2.0d);
        output.writeFloat(3.05f);
        output.writeInt(32);
        output.writeLong(64L);
        output.writeShort((short) 16);
        output.writeBytes(new String("afdsafdsafsa").getBytes(), 6, 4);
        output.writeObject(new String[]{"abc", "def", "gfk"});
        output.writeObject(new int[]{4,5,8});
        output.writeObject(new JacksonBean());
        output.flushBuffer();

        JacksonObjectInput input = new JacksonObjectInput(new ByteArrayInputStream(bos.toByteArray()));
        System.out.println(input.readUTF());
        System.out.println(input.readBool());
        System.out.println(input.readByte());
        System.out.println(new String(input.readBytes()));
        System.out.println(input.readDouble());
        System.out.println(input.readFloat());
        System.out.println(input.readInt());
        System.out.println(input.readLong());
        System.out.println(input.readShort());
        System.out.println(Arrays.toString(input.readBytes()));
        System.out.println(Arrays.toString(input.readObject(String[].class)));
        System.out.println(Arrays.toString(input.readObject(int[].class)));
        JacksonBean jacksonBean = input.readObject(JacksonBean.class);
        System.out.println(jacksonBean);

    }
    public static class JacksonBean {
        private boolean bValue = true;
        private String sValue = "string";
        private int iValue = 20;
        private double dValue = 20.5;
        private float fValue = 2.18f;
        private long lValue = 3;
        private Double dValue2 = null;
        private List<InnerBean> innerBeanList = new ArrayList<InnerBean>();

        public JacksonBean(){
            innerBeanList.add(new InnerBean());
            innerBeanList.add(new InnerBean());
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

        public List<InnerBean> getInnerBeanList() {
            return innerBeanList;
        }

        public void setInnerBeanList(List<InnerBean> innerBeanList) {
            this.innerBeanList = innerBeanList;
        }
    }

    public static class InnerBean {
        private String siValue = "innerStr";
        private int iiValue = 18;

        public String getSiValue() {
            return siValue;
        }

        public void setSiValue(String siValue) {
            this.siValue = siValue;
        }

        public int getIiValue() {
            return iiValue;
        }

        public void setIiValue(int iiValue) {
            this.iiValue = iiValue;
        }

        @Override
        public String toString() {
            return "InnerBean{" +
                    "siValue='" + siValue + '\'' +
                    ", iiValue=" + iiValue +
                    '}';
        }
    }
}
