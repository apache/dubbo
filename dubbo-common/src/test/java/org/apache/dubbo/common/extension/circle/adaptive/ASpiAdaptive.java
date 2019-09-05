package org.apache.dubbo.common.extension.circle.adaptive;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.circle.ASpi;
import org.apache.dubbo.common.extension.circle.BSpi;


@Adaptive
public class ASpiAdaptive implements ASpi {

    private BSpi BSpi;

    public BSpi getBSpi() {
        return BSpi;
    }

    public void setBSpi(BSpi BSpi) {
        this.BSpi = BSpi;
    }
}
