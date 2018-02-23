package com.alibaba.com.caucho.hessian.io;

import java.io.Serializable;
import java.util.Map;

/**
 *
 * test short serialize & deserialize model
 *
 * @author jason.shang
 */
public class Hessian2StringShortType implements Serializable {

    Map<String, Short> stringShortMap;

    Map<String, Byte> stringByteMap;

    Map<String, PersonType> stringPersonTypeMap;

    public Hessian2StringShortType(){

    }
}
