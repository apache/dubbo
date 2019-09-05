package org.apache.dubbo.common.extension.circle.support;

import org.apache.dubbo.common.extension.circle.ASpi;
import org.apache.dubbo.common.extension.circle.BSpi;


public class ASpiSupport implements ASpi {
    @Override
    public BSpi getBSpi() {
        return null;
    }

    @Override
    public void setBSpi(BSpi BSpi) {

    }
}
