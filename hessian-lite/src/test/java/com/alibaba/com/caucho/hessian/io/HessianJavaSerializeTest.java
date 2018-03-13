package com.alibaba.com.caucho.hessian.io;

import com.alibaba.com.caucho.hessian.io.base.SerializeTestBase;
import com.alibaba.com.caucho.hessian.io.beans.BaseUser;
import com.alibaba.com.caucho.hessian.io.beans.GrandsonUser;
import com.alibaba.com.caucho.hessian.io.beans.SubUser;

import org.junit.Assert;
import org.junit.Test;

/**
 * fix hession serialize bug:
 * the filed of parent class will cover the filed of sub class
 *
 */
public class HessianJavaSerializeTest extends SerializeTestBase {

    @Test
    public void testGetBaseUserName() throws Exception {

        BaseUser baseUser = new BaseUser();
        baseUser.setUserId(1);
        baseUser.setUserName("tom");

        BaseUser serializedUser = baseHessionSerialize(baseUser);
        Assert.assertEquals("tom", serializedUser.getUserName());
    }


    @Test
    public void testGetSubUserName() throws Exception {
        SubUser subUser = new SubUser();
        subUser.setUserId(1);
        subUser.setUserName("tom");

        SubUser serializedUser = baseHessionSerialize(subUser);
        Assert.assertEquals("tom", serializedUser.getUserName());

    }

    @Test
    public void testGetGrandsonUserName() throws Exception {
        GrandsonUser grandsonUser = new GrandsonUser();
        grandsonUser.setUserId(1);
        grandsonUser.setUserName("tom");

        GrandsonUser serializedUser = baseHessionSerialize(grandsonUser);
        Assert.assertEquals("tom", serializedUser.getUserName());
    }

}
