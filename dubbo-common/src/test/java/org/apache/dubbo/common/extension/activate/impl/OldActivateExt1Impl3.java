package org.apache.dubbo.common.extension.activate.impl;

import com.alibaba.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.activate.ActivateExt1;

@Activate(group = "old_group")
public class OldActivateExt1Impl3 implements ActivateExt1 {
    public String echo(String msg) {
        return msg;
    }
}
