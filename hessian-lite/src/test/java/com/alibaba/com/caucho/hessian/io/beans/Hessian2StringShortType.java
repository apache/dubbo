package com.alibaba.com.caucho.hessian.io.beans;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * test short serialize & deserialize model
 *
 * @author jason.shang
 */
public class Hessian2StringShortType implements Serializable {

    public Map<String, Short> stringShortMap;

    public Map<String, Byte> stringByteMap;

    public Map<String, PersonType> stringPersonTypeMap;

    public Hessian2StringShortType(){

    }
}
