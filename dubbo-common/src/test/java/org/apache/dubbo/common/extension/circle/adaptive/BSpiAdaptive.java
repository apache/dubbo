package org.apache.dubbo.common.extension.circle.adaptive;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.circle.BSpi;


@Adaptive
public class BSpiAdaptive implements BSpi {

    private org.apache.dubbo.common.extension.circle.ASpi ASpi;

    @Override
    public org.apache.dubbo.common.extension.circle.ASpi getASpi() {
        return ASpi;
    }

    public void setASpi(org.apache.dubbo.common.extension.circle.ASpi ASpi) {
        this.ASpi = ASpi;
    }
}
