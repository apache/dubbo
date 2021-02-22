package org.apache.dubbo.common.extension.activate.impl;

import org.apache.dubbo.common.extension.activate.ActivateExt1;

/**
 * @author shaoyu
 */
public class ActivateExt1Wrapper implements ActivateExt1 {
    private ActivateExt1 activateExt1;

    public ActivateExt1Wrapper(ActivateExt1 activateExt1){
        this.activateExt1 = activateExt1;
    }
    @Override
    public String echo(String msg) {
        return activateExt1.echo(msg);
    }
}
